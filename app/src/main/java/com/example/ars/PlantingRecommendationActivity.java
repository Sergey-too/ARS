package com.example.ars;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.adapters.PlantingAdapter;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.*;
import com.example.ars.utils.SharedPreferencesHelper;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlantingRecommendationActivity extends AppCompatActivity {

    private ApiService apiService;
    private SharedPreferencesHelper prefsHelper;
    private List<UserCrop> userCrops = new ArrayList<>();
    private Map<Integer, WeatherResponse> weatherByRegion = new HashMap<>();
    private PlantingAdapter adapter;

    private TextView tvEmpty, tvResultsTitle;
    private RecyclerView rvRecommendations;
    private MaterialButton btnRefresh;
    private Double windMax;
    private Double humMin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_planting_recommendation);

        prefsHelper = new SharedPreferencesHelper(this);
        apiService = RetrofitClient.getApiService();

        tvEmpty = findViewById(R.id.tvEmpty);
        tvResultsTitle = findViewById(R.id.tvResultsTitle);
        rvRecommendations = findViewById(R.id.rvRecommendations);
        btnRefresh = findViewById(R.id.btnRefresh);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnRefresh.setOnClickListener(v -> refreshData());

        rvRecommendations.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PlantingAdapter(new ArrayList<>());
        rvRecommendations.setAdapter(adapter);

        loadUserCropsAndWeather();
    }

    private void refreshData() {
        userCrops.clear();
        weatherByRegion.clear();
        loadUserCropsAndWeather();
    }

    private void loadUserCropsAndWeather() {
        User currentUser = prefsHelper.getUser();
        if (currentUser == null) return;

        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText("Анализируем условия для ваших участков...");

        apiService.getUserCrops(currentUser.getId()).enqueue(new Callback<List<UserCrop>>() {
            @Override
            public void onResponse(Call<List<UserCrop>> call, Response<List<UserCrop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userCrops = response.body();
                    if (userCrops.isEmpty()) {
                        tvEmpty.setText("У вас пока нет растений на участках");
                        return;
                    }
                    loadWeatherForAllRegions();
                } else {
                    tvEmpty.setText("Ошибка загрузки данных");
                }
            }

            @Override
            public void onFailure(Call<List<UserCrop>> call, Throwable t) {
                tvEmpty.setText("Ошибка сети: проверьте соединение");
            }
        });
    }

    private void loadWeatherForAllRegions() {
        Set<String> uniqueRegions = new HashSet<>();
        for (UserCrop uc : userCrops) {
            if (uc.getArea() != null && uc.getArea().getRegion() != null) {
                uniqueRegions.add(uc.getArea().getRegion().getName());
            }
        }

        if (uniqueRegions.isEmpty()) {
            tvEmpty.setText("Ни для одного участка не указан регион");
            return;
        }

        for (String regionName : uniqueRegions) {
            loadWeatherForRegion(regionName, uniqueRegions.size());
        }
    }

    private void loadWeatherForRegion(String name, int totalExpected) {
        apiService.getWeatherForRegion(name).enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    weatherByRegion.put(name.hashCode(), response.body());

                    if (weatherByRegion.size() == totalExpected) {
                        analyzePlantingDates();
                    }
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Log.e("Weather", "Failed to load weather for " + name);
            }
        });
    }

    private void analyzePlantingDates() {
        List<PlantingRecommendation> recommendations = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            String dateStr = sdf.format(calendar.getTime());

            for (UserCrop uc : userCrops) {
                if (uc.getArea() == null || uc.getArea().getRegion() == null) continue;

                String regionName = uc.getArea().getRegion().getName();
                WeatherResponse wr = weatherByRegion.get(regionName.hashCode());

                if (wr != null && wr.getWeather() != null) {
                    for (WeatherData wd : wr.getWeather()) {
                        if (wd.getDate().equals(dateStr)) {
                            if (isGoodForPlanting(uc.getCrop(), wd)) {
                                PlantingRecommendation rec = new PlantingRecommendation();
                                rec.setDate(dateStr);
                                rec.setCropName(uc.getCrop().getName() + " (" + uc.getArea().getName() + ")");
                                rec.setRegionName(regionName);
                                rec.setGoodDay(true);
                                rec.setTempMin(wd.getTempMin());
                                rec.setTempMax(wd.getTempMax());
                                rec.setHumMin(wd.getHumMin());
                                rec.setWindMax(wd.getWindMax());
                                rec.setReason(generateReason(uc.getCrop(), wd));
                                recommendations.add(rec);
                            }
                        }
                    }
                }
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        showResults(recommendations);
    }
    private String generateReason(Crop crop, WeatherData wd) {
        if (crop.getMinTemp() == null) return "Идеальные условия для посадки";

        return String.format(Locale.getDefault(),
                "Требуется от %.0f°C. Ожидается %.1f°C",
                (double) crop.getMinTemp(), wd.getTempMin());
    }

    private boolean isGoodForPlanting(Crop crop, WeatherData wd) {
        if (crop == null || wd == null) return false;

        boolean tempOk = (crop.getMinTemp() == null || wd.getTempMin() >= crop.getMinTemp().doubleValue());

        boolean windOk = (crop.getMaxWind() == null || wd.getWindMax() <= crop.getMaxWind().doubleValue());

        boolean rainOk = (wd.getPrecipitation() < 10.0);

        return tempOk && windOk && rainOk;
    }

    private void showResults(List<PlantingRecommendation> list) {
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        if (list.isEmpty()) {
            tvEmpty.setText("На ближайшую неделю подходящих дней для посадки не найдено");
        }

        rvRecommendations.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        tvResultsTitle.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);

        Collections.sort(list, (o1, o2) -> o1.getDate().compareTo(o2.getDate()));

        adapter.updateData(list);
    }
}