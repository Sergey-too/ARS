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
import com.example.ars.models.Crop;
import com.example.ars.models.PlantingRecommendation;
import com.example.ars.models.UserCrop;
import com.example.ars.models.WeatherData;
import com.example.ars.models.WeatherResponse;
import com.example.ars.utils.SharedPreferencesHelper;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlantingRecommendationActivity extends AppCompatActivity {

    private ApiService apiService;
    private SharedPreferencesHelper prefsHelper;
    private List<UserCrop> userCrops = new ArrayList<>();
    private Map<Integer, WeatherResponse> weatherByRegion = new HashMap<>();
    private PlantingAdapter adapter;

    private TextView tvEmpty;
    private TextView tvResultsTitle;
    private RecyclerView rvRecommendations;
    private MaterialButton btnRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_planting_recommendation);

        // Инициализация
        prefsHelper = new SharedPreferencesHelper(this);
        apiService = RetrofitClient.getApiService();

        // Найти UI элементы
        tvEmpty = findViewById(R.id.tvEmpty);
        tvResultsTitle = findViewById(R.id.tvResultsTitle);
        rvRecommendations = findViewById(R.id.rvRecommendations);
        btnRefresh = findViewById(R.id.btnRefresh);

        // Кнопка назад
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Кнопка обновления
        btnRefresh.setOnClickListener(v -> refreshData());

        // Настройка RecyclerView
        rvRecommendations.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PlantingAdapter(new ArrayList<PlantingRecommendation>());
        rvRecommendations.setAdapter(adapter);

        loadUserCropsAndWeather();
    }

    private void refreshData() {
        showLoading();
        userCrops.clear();
        weatherByRegion.clear();
        loadUserCropsAndWeather();
    }

    private void showLoading() {
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText("Анализируем данные...");
        rvRecommendations.setVisibility(View.GONE);
        btnRefresh.setVisibility(View.GONE);
        tvResultsTitle.setVisibility(View.GONE);
    }

    private void showResults(List<PlantingRecommendation> recommendations) {
        if (recommendations.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvRecommendations.setVisibility(View.GONE);
            tvEmpty.setText("В ближайшие 6 дней нет благоприятных дней для посадки ваших растений");
        } else {
            tvEmpty.setVisibility(View.GONE);
            tvResultsTitle.setVisibility(View.VISIBLE);
            rvRecommendations.setVisibility(View.VISIBLE);

            adapter.updateData(recommendations);
            rvRecommendations.post(() -> {
                rvRecommendations.invalidate();
                rvRecommendations.requestLayout();
            });

            Log.d("PLANTING", "Показано результатов: " + recommendations.size());
        }

        btnRefresh.setVisibility(View.VISIBLE);
    }

    private void loadUserCropsAndWeather() {
        com.example.ars.models.User currentUser = prefsHelper.getUser();
        if (currentUser == null || currentUser.getId() == null) {
            showError("Пользователь не авторизован");
            return;
        }

        apiService.getUserCrops(currentUser.getId()).enqueue(new Callback<List<UserCrop>>() {
            @Override
            public void onResponse(Call<List<UserCrop>> call, Response<List<UserCrop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userCrops = response.body();

                    if (userCrops.isEmpty()) {
                        showError("У вас нет растений в коллекции. Добавьте растения чтобы получить рекомендации");
                        return;
                    }

                    loadWeatherForAllRegions();
                } else {
                    showError("Ошибка загрузки растений");
                }
            }

            @Override
            public void onFailure(Call<List<UserCrop>> call, Throwable t) {
                showError("Ошибка сети: " + t.getMessage());
            }
        });
    }

    private void showError(String message) {
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText("❌ " + message);
        btnRefresh.setVisibility(View.VISIBLE);
    }

    private void loadWeatherForAllRegions() {
        Map<String, Boolean> processedRegions = new HashMap<>();

        for (UserCrop userCrop : userCrops) {
            if (userCrop.getRegion() != null && userCrop.getRegion().getName() != null) {
                String regionName = userCrop.getRegion().getName();
                if (!processedRegions.containsKey(regionName)) {
                    processedRegions.put(regionName, true);
                    loadWeatherForRegion(regionName);
                }
            }
        }

        if (processedRegions.isEmpty()) {
            showError("Не указаны регионы для ваших растений");
        }
    }

    private void loadWeatherForRegion(String regionName) {
        apiService.getWeatherForRegion(regionName).enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weatherResponse = response.body();
                    weatherByRegion.put(regionName.hashCode(), weatherResponse);

                    int loaded = weatherByRegion.size();
                    int total = 0;
                    for (UserCrop userCrop : userCrops) {
                        if (userCrop.getRegion() != null && userCrop.getRegion().getName() != null) {
                            total++;
                        }
                    }

                    boolean allLoaded = checkIfAllRegionsLoaded();

                    if (allLoaded) {
                        analyzePlantingDates();
                    }
                } else {
                    weatherByRegion.put(regionName.hashCode(), null);
                    checkIfAllRegionsLoaded();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                weatherByRegion.put(regionName.hashCode(), null);
                checkIfAllRegionsLoaded();
            }
        });
    }

    private boolean checkIfAllRegionsLoaded() {
        int totalRegions = 0;
        for (UserCrop userCrop : userCrops) {
            if (userCrop.getRegion() != null && userCrop.getRegion().getName() != null) {
                totalRegions++;
                String regionName = userCrop.getRegion().getName();
                if (!weatherByRegion.containsKey(regionName.hashCode())) {
                    return false;
                }
            }
        }
        return totalRegions > 0;
    }


    private void analyzePlantingDates() {
        List<PlantingRecommendation> recommendations = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int i = 0; i < 6; i++) {
            Calendar tempCalendar = (Calendar) calendar.clone();
            tempCalendar.add(Calendar.DAY_OF_YEAR, i);
            String date = sdf.format(tempCalendar.getTime());

            for (UserCrop userCrop : userCrops) {
                if (userCrop.getCrop() != null && userCrop.getRegion() != null) {
                    Crop crop = userCrop.getCrop();
                    String regionName = userCrop.getRegion().getName();
                    WeatherResponse weatherResponse = weatherByRegion.get(regionName.hashCode());

                    if (weatherResponse != null && weatherResponse.getWeather() != null) {
                        // Ищем погоду на эту дату
                        WeatherData weatherForDate = null;
                        for (WeatherData weather : weatherResponse.getWeather()) {
                            if (weather.getDate().equals(date)) {
                                weatherForDate = weather;
                                break;
                            }
                        }

                        if (weatherForDate != null) {
                            boolean isGood = isGoodForPlanting(crop, weatherForDate);

                            if (isGood) {
                                PlantingRecommendation rec = new PlantingRecommendation();
                                rec.setDate(date);
                                rec.setCropName(crop.getName());
                                rec.setRegionName(regionName);
                                rec.setReason(generateReason(crop, weatherForDate));
                                rec.setWeatherTemperature(weatherForDate.getTemperature());
                                rec.setWeatherHumidity(weatherForDate.getHumidity());
                                rec.setWeatherWind(weatherForDate.getWind());
                                rec.setGoodDay(true);

                                recommendations.add(rec);
                            }
                        }
                    }
                }
            }
        }

        showResults(recommendations);
    }

    private boolean isGoodForPlanting(Crop crop, WeatherData weather) {
        try {
            String tempStr = weather.getTemperature().replace("°C", "").trim();

            // Парсим диапазон температур
            if (tempStr.contains("..")) {
                String[] parts = tempStr.split("\\.\\.");
                float minTemp = Float.parseFloat(parts[0]); // -15
                float maxTemp = Float.parseFloat(parts[1]); // -6
                float avgTemp = (minTemp + maxTemp) / 2;

                // Проверяем подходит ли средняя температура для растения
                boolean tempGood = (crop.getMinTemp() == null || avgTemp >= crop.getMinTemp()) &&
                        (crop.getMaxTemp() == null || avgTemp <= crop.getMaxTemp());

                return tempGood;
            }
            return false;

        } catch (Exception e) {
            Log.e("PLANTING", "Ошибка анализа погоды: " + e.getMessage());
            return false;
        }
    }

    private String generateReason(Crop crop, WeatherData weather) {
        StringBuilder reason = new StringBuilder();

        try {
            String tempStr = weather.getTemperature().replace("°C", "").trim();
            float temp = Float.parseFloat(tempStr);

            String humidityStr = weather.getHumidity().replace("%", "").trim();
            int humidity = Integer.parseInt(humidityStr);

            reason.append("Температура: ").append(temp).append("°C ");
            if (crop.getMinTemp() != null && crop.getMaxTemp() != null) {
                reason.append("(нужно: ").append(crop.getMinTemp()).append("-")
                        .append(crop.getMaxTemp()).append("°C)");
            }

            reason.append("\nВлажность: ").append(humidity).append("% ");
            if (crop.getMinHumidity() != null && crop.getMaxHumidity() != null) {
                reason.append("(нужно: ").append(crop.getMinHumidity()).append("-")
                        .append(crop.getMaxHumidity()).append("%%)");
            }

        } catch (Exception e) {
            reason.append("Благоприятные условия для посадки");
        }

        return reason.toString();
    }
}