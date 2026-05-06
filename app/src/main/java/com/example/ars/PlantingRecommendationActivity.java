package com.example.ars;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
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

public class PlantingRecommendationActivity extends AppCompatActivity {

    private static final String TAG = "PlantingRecommend";

    private ApiService apiService;
    private SharedPreferencesHelper prefsHelper;
    private List<UserCrop> userCrops = new ArrayList<>();
    private Map<String, List<WeatherData>> weatherByRegion = new HashMap<>();
    private PlantingAdapter adapter;

    private TextView tvEmpty, tvResultsTitle;
    private RecyclerView rvRecommendations;
    private MaterialButton btnRefresh;
    private ProgressBar progressBar;

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
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnRefresh.setOnClickListener(v -> refreshData());

        rvRecommendations.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PlantingAdapter();
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
        if (currentUser == null) {
            tvEmpty.setText("Пользователь не авторизован");
            return;
        }

        showLoading(true);
        tvEmpty.setText("Анализируем условия для ваших участков...");
        rvRecommendations.setVisibility(View.GONE);
        tvResultsTitle.setVisibility(View.GONE);

        apiService.getUserCrops(currentUser.getId()).enqueue(new retrofit2.Callback<List<UserCrop>>() {
            @Override
            public void onResponse(retrofit2.Call<List<UserCrop>> call, retrofit2.Response<List<UserCrop>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    userCrops = response.body();
                    loadWeatherForAllRegions();
                } else {
                    showLoading(false);
                    tvEmpty.setText("У вас пока нет растений на участках");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<UserCrop>> call, Throwable t) {
                showLoading(false);
                tvEmpty.setText("Ошибка сети: проверьте соединение");
                Log.e(TAG, "Ошибка: " + t.getMessage());
            }
        });
    }

    private void loadWeatherForAllRegions() {
        Set<Integer> uniqueRegionIds = new HashSet<>();
        for (UserCrop uc : userCrops) {
            if (uc.getArea() != null && uc.getArea().getRegion() != null && uc.getArea().getRegion().getId() != null) {
                uniqueRegionIds.add(uc.getArea().getRegion().getId().intValue());
            }
        }

        if (uniqueRegionIds.isEmpty()) {
            showLoading(false);
            tvEmpty.setText("Ни для одного участка не указан регион");
            return;
        }

        tvEmpty.setText("Загрузка погоды для " + uniqueRegionIds.size() + " регионов...");

        for (Integer regionId : uniqueRegionIds) {
            loadWeatherForRegion(regionId, uniqueRegionIds.size());
        }
    }

    private void loadWeatherForRegion(Integer regionId, int totalExpected) {
        apiService.getWeatherByRegionId(regionId).enqueue(new retrofit2.Callback<WeatherResponse>() {
            @Override
            public void onResponse(retrofit2.Call<WeatherResponse> call, retrofit2.Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getWeather() != null) {
                    weatherByRegion.put(String.valueOf(regionId), response.body().getWeather());

                    if (weatherByRegion.size() == totalExpected) {
                        analyzePlantingDates();
                    }
                } else {
                    if (weatherByRegion.size() + 1 == totalExpected) {
                        analyzePlantingDates();
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<WeatherResponse> call, Throwable t) {
                if (weatherByRegion.size() + 1 == totalExpected) {
                    analyzePlantingDates();
                }
            }
        });
    }

    private void analyzePlantingDates() {
        List<PlantingRecommendation> recommendations = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM", new Locale("ru"));
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("ru"));

        for (int i = 0; i < 7; i++) {
            String dateStr = sdf.format(calendar.getTime());
            String displayDate = "";
            String dayOfWeek = "";

            try {
                Date d = sdf.parse(dateStr);
                displayDate = dateFormat.format(d);
                dayOfWeek = dayFormat.format(d);
            } catch (Exception e) {
                displayDate = dateStr;
                dayOfWeek = "";
            }

            for (UserCrop uc : userCrops) {
                if (uc.getArea() == null || uc.getArea().getRegion() == null) continue;
                if (uc.getCrop() == null) continue;

                String regionKey = String.valueOf(uc.getArea().getRegion().getId().intValue());
                List<WeatherData> weatherList = weatherByRegion.get(regionKey);

                if (weatherList != null) {
                    for (WeatherData wd : weatherList) {
                        if (wd.getDate() != null && wd.getDate().equals(dateStr)) {

                            PlantingRecommendation rec = new PlantingRecommendation();
                            rec.setDate(displayDate);
                            rec.setDayOfWeek(dayOfWeek);

                            String cropName = uc.getCrop().getName();
                            if (uc.getCrop().getVariety() != null && !uc.getCrop().getVariety().isEmpty()) {
                                cropName += " (" + uc.getCrop().getVariety() + ")";
                            }
                            rec.setCropName(cropName);
                            rec.setAreaName(uc.getArea().getName());

                            double tempMin = parseDouble(wd.getTemperatureMin());
                            double tempMax = parseDouble(wd.getTemperatureMax());
                            double humMin = parseDouble(wd.getHumidityMin());
                            double windMax = parseDouble(wd.getWindMax());

                            String weatherText = "🌡️ " + (int)tempMin + ".." + (int)tempMax + "°C  💧 " + (int)humMin + "-" + ((int)humMin + 10) + "%  💨 " + ((int)windMax - 1) + "-" + (int)windMax + " м/с";
                            rec.setWeatherText(weatherText);

                            double cropMinTemp = uc.getCrop().getMinTemp() != null ? uc.getCrop().getMinTemp() : 0;
                            double cropMaxTemp = uc.getCrop().getMaxTemp() != null ? uc.getCrop().getMaxTemp() : 0;
                            double cropMinHum = uc.getCrop().getMinHumidity() != null ? uc.getCrop().getMinHumidity() : 0;
                            double cropMaxHum = uc.getCrop().getMaxHumidity() != null ? uc.getCrop().getMaxHumidity() : 0;

                            String reason = "Температура: " + String.format("%.1f", tempMin) + "°C (нужно: " + (int)cropMinTemp + "-" + (int)cropMaxTemp + "°C)\nВлажность: " + (int)humMin + "% (нужно: " + (int)cropMinHum + "-" + (int)cropMaxHum + "%)";
                            rec.setReason(reason);

                            rec.setGoodDay(isGoodForPlanting(uc.getCrop(), wd));
                            recommendations.add(rec);
                            break;
                        }
                    }
                }
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        showLoading(false);
        showResults(recommendations);
    }

    private double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0.0;
        try {
            String clean = value.replace(",", ".");
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private boolean isGoodForPlanting(Crop crop, WeatherData wd) {
        if (crop == null || wd == null) return false;

        boolean tempOk = true;
        if (crop.getMinTemp() != null) {
            double minTemp = crop.getMinTemp();
            double currentTemp = parseDouble(wd.getTemperatureMin());
            tempOk = currentTemp >= minTemp;
        }

        boolean windOk = true;
        if (crop.getMaxWind() != null) {
            double maxWind = crop.getMaxWind();
            double currentWind = parseDouble(wd.getWindMax());
            windOk = currentWind <= maxWind;
        }

        double precipitation = parseDouble(wd.getPrecipitation());
        boolean rainOk = precipitation < 10.0;

        return tempOk && windOk && rainOk;
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvRecommendations.setVisibility(View.GONE);
            tvResultsTitle.setVisibility(View.GONE);
        }
    }

    private void showResults(List<PlantingRecommendation> list) {
        if (list.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("На ближайшую неделю подходящих дней для посадки не найдено");
            rvRecommendations.setVisibility(View.GONE);
            tvResultsTitle.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvRecommendations.setVisibility(View.VISIBLE);
            tvResultsTitle.setVisibility(View.VISIBLE);
            tvResultsTitle.setText("Рекомендуемые дни для посадки");
            adapter.updateData(list);
        }
    }
}