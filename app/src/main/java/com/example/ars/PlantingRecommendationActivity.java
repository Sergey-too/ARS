package com.example.ars;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText("Анализируем данные...");
        userCrops.clear();
        weatherByRegion.clear();
        loadUserCropsAndWeather();
    }

    private void loadUserCropsAndWeather() {
        User currentUser = prefsHelper.getUser();
        if (currentUser == null) return;

        apiService.getUserCrops(currentUser.getId()).enqueue(new Callback<List<UserCrop>>() {
            @Override
            public void onResponse(Call<List<UserCrop>> call, Response<List<UserCrop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userCrops = response.body();
                    if (userCrops.isEmpty()) {
                        tvEmpty.setText("Коллекция пуста");
                        return;
                    }
                    loadWeatherForAllRegions();
                }
            }
            @Override
            public void onFailure(Call<List<UserCrop>> call, Throwable t) {
                tvEmpty.setText("Ошибка сети");
            }
        });
    }

    private void loadWeatherForAllRegions() {
        Set<String> regions = new HashSet<>();
        for (UserCrop uc : userCrops) {
            if (uc.getRegion() != null) regions.add(uc.getRegion().getName());
        }
        for (String name : regions) {
            loadWeatherForRegion(name, regions.size());
        }
    }

    private void loadWeatherForRegion(String name, int total) {
        apiService.getWeatherForRegion(name).enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    weatherByRegion.put(name.hashCode(), response.body());
                    if (weatherByRegion.size() == total) analyzePlantingDates();
                }
            }
            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {}
        });
    }

    private void analyzePlantingDates() {
        List<PlantingRecommendation> recommendations = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            String date = sdf.format(calendar.getTime());
            for (UserCrop uc : userCrops) {
                String rName = uc.getRegion().getName();
                WeatherResponse wr = weatherByRegion.get(rName.hashCode());
                if (wr != null && wr.getWeather() != null) {
                    for (WeatherData wd : wr.getWeather()) {
                        if (wd.getDate().equals(date)) {
                            if (isGoodForPlanting(uc.getCrop(), wd)) {
                                PlantingRecommendation rec = new PlantingRecommendation();
                                rec.setDate(date);
                                rec.setCropName(uc.getCrop().getName());
                                rec.setRegionName(rName);
                                rec.setGoodDay(true);
                                rec.setTempMin(wd.getTempMin());
                                rec.setTempMax(wd.getTempMax());
                                rec.setHumMin(wd.getHumMin());
                                rec.setHumMax(wd.getHumMax());
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

    private boolean isGoodForPlanting(Crop crop, WeatherData wd) {
        boolean tempOk = (crop.getMinTemp() == null || wd.getTempMin() >= crop.getMinTemp()) &&
                (crop.getMaxTemp() == null || wd.getTempMax() <= crop.getMaxTemp());
        boolean windOk = (crop.getMaxWind() == null || wd.getWindMax() <= crop.getMaxWind());
        return tempOk && windOk;
    }

    private String generateReason(Crop crop, WeatherData wd) {
        return String.format(Locale.getDefault(),
                "Оптимально: %s°C...%s°C\nТекущая: %.1f°C...%.1f°C",
                crop.getMinTemp(), crop.getMaxTemp(), wd.getTempMin(), wd.getTempMax());
    }

    private void showResults(List<PlantingRecommendation> list) {
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        rvRecommendations.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        tvResultsTitle.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        adapter.updateData(list);
    }
}