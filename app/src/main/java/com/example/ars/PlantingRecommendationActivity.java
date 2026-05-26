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

    private ApiService apiService;
    private SharedPreferencesHelper prefsHelper;
    private List<UserCrop> userCrops = new ArrayList<>();
    private final Set<String> plantedKeys = new HashSet<>();
    private final Map<String, List<WeatherData>> weatherByRegion = new HashMap<>();
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
        adapter.setOnPlantClickListener((item, position) -> onPlantClick(item));
        rvRecommendations.setAdapter(adapter);

        loadData();
    }

    private void refreshData() {
        runOnUiThread(() -> {
            userCrops.clear();
            plantedKeys.clear();
            weatherByRegion.clear();
            adapter.updateData(new ArrayList<>());
        });
        loadData();
    }

    private void loadData() {
        User currentUser = prefsHelper.getUser();
        if (currentUser == null) {
            tvEmpty.setText("Пользователь не авторизован");
            return;
        }

        showLoading(true);

        apiService.getUserCrops(currentUser.getId()).enqueue(new retrofit2.Callback<List<UserCrop>>() {
            @Override
            public void onResponse(retrofit2.Call<List<UserCrop>> call, retrofit2.Response<List<UserCrop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userCrops = response.body();
                    loadPlantedHistory();
                } else {
                    showLoading(false);
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("У вас нет растений");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<UserCrop>> call, Throwable t) {
                showLoading(false);
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Ошибка: " + t.getMessage());
            }
        });
    }

    private void loadPlantedHistory() {
        User currentUser = prefsHelper.getUser();
        if (currentUser == null) return;

        apiService.getPlantingHistory(currentUser.getId()).enqueue(new retrofit2.Callback<List<GardenHistory>>() {
            @Override
            public void onResponse(retrofit2.Call<List<GardenHistory>> call, retrofit2.Response<List<GardenHistory>> response) {
                plantedKeys.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (GardenHistory h : response.body()) {
                        Integer actionTypeId = h.getActionTypeId();

                        if (actionTypeId != null && actionTypeId == 1 && h.getCropName() != null && h.getAreaName() != null) {
                            String cropName = h.getCropName().trim().toLowerCase();
                            String variety = h.getVariety() != null ? h.getVariety().trim().toLowerCase() : "обычный";
                            if (variety.isEmpty()) variety = "обычный";
                            String areaName = h.getAreaName().trim().toLowerCase();

                            String key = cropName + "|" + variety + "|" + areaName;
                            plantedKeys.add(key);
                        }
                    }
                }
                loadWeatherForAllRegions();
            }

            @Override
            public void onFailure(retrofit2.Call<List<GardenHistory>> call, Throwable t) {
                loadWeatherForAllRegions();
            }
        });
    }

    private void loadWeatherForAllRegions() {
        weatherByRegion.clear();
        Set<Integer> uniqueRegionIds = new HashSet<>();
        for (UserCrop uc : userCrops) {
            if (uc.getArea() != null) {
                Integer regionId = uc.getArea().getRegionId();
                if (regionId != null) {
                    uniqueRegionIds.add(regionId);
                }
            }
        }

        if (uniqueRegionIds.isEmpty()) {
            showLoading(false);
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("У ваших участков не указан регион");
            return;
        }

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
                }
                if (weatherByRegion.size() == totalExpected) {
                    analyzeRecommendations();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<WeatherResponse> call, Throwable t) {
                weatherByRegion.put(String.valueOf(regionId), new ArrayList<>());
                if (weatherByRegion.size() == totalExpected) {
                    analyzeRecommendations();
                }
            }
        });
    }

    private void analyzeRecommendations() {
        List<PlantingRecommendation> recommendations = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM", new Locale("ru"));
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("ru"));

        Calendar calendar = Calendar.getInstance();

        List<UserCrop> activeCropsToRecommend = new ArrayList<>();

        int currentMonth = calendar.get(Calendar.MONTH) + 1;
        boolean isWinter = (currentMonth == 12 || currentMonth == 1 || currentMonth == 2);

        for (UserCrop uc : userCrops) {
            if (uc.getArea() == null || uc.getCrop() == null) {
                continue;
            }

            boolean hasPlantedDate = uc.getPlantedAt() != null && !uc.getPlantedAt().isEmpty();
            boolean hasHarvestedDate = uc.getHarvestedAt() != null && !uc.getHarvestedAt().isEmpty();

            if (hasPlantedDate && hasHarvestedDate) {
                Log.d("RECOMMEND", "Пропускаем " + uc.getCrop().getName() + " - уже запланировано с датами");
                continue;
            }

            String cropName = uc.getCrop().getName().trim().toLowerCase();
            String variety = uc.getCrop().getVariety() != null ? uc.getCrop().getVariety().trim().toLowerCase() : "обычный";
            if (variety.isEmpty()) variety = "обычный";
            String areaName = uc.getArea().getName().trim().toLowerCase();

            String checkKey = cropName + "|" + variety + "|" + areaName;

            if (plantedKeys.contains(checkKey)) {
                continue;
            }

            activeCropsToRecommend.add(uc);
        }

        if (isWinter) {
            List<UserCrop> filteredCrops = new ArrayList<>();
            for (UserCrop uc : activeCropsToRecommend) {
                Crop crop = uc.getCrop();
                // Если растение нельзя сажать через рассаду - пропускаем
                if (crop.getCanSeedlings() == null || !crop.getCanSeedlings()) {
                    Log.d("RECOMMEND", "Пропускаем " + crop.getName() + " - зимой нельзя сажать без рассады");
                    continue;
                }
                filteredCrops.add(uc);
            }
            activeCropsToRecommend = filteredCrops;

            if (activeCropsToRecommend.isEmpty()) {
                runOnUiThread(() -> {
                    showLoading(false);
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvRecommendations.setVisibility(View.GONE);
                    tvResultsTitle.setVisibility(View.GONE);
                    tvEmpty.setText("Зимой можно сажать только растения через рассаду");
                });
                return;
            }
        }

        if (activeCropsToRecommend.isEmpty()) {
            runOnUiThread(() -> {
                showLoading(false);
                tvEmpty.setVisibility(View.VISIBLE);
                rvRecommendations.setVisibility(View.GONE);
                tvResultsTitle.setVisibility(View.GONE);
                tvEmpty.setText("Нет культур для посадок в данный момент");
            });
            return;
        }

        boolean hasRegion = false;
        for (UserCrop uc : activeCropsToRecommend) {
            if (uc.getArea().getRegion() != null && uc.getArea().getRegion().getId() != null) {
                hasRegion = true;
                break;
            }
        }

        if (!hasRegion) {
            runOnUiThread(() -> {
                showLoading(false);
                tvEmpty.setVisibility(View.VISIBLE);
                rvRecommendations.setVisibility(View.GONE);
                tvResultsTitle.setVisibility(View.GONE);
                tvEmpty.setText("У ваших участков не указан регион");
            });
            return;
        }

        if (weatherByRegion.isEmpty()) {
            runOnUiThread(() -> {
                showLoading(false);
                tvEmpty.setVisibility(View.VISIBLE);
                rvRecommendations.setVisibility(View.GONE);
                tvResultsTitle.setVisibility(View.GONE);
                tvEmpty.setText("Нет данных о погоде для вашего региона");
            });
            return;
        }

        for (int day = 0; day < 7; day++) {
            String dateStr = sdf.format(calendar.getTime());
            String displayDate = dateFormat.format(calendar.getTime());
            String dayOfWeek = dayFormat.format(calendar.getTime());

            for (UserCrop uc : activeCropsToRecommend) {
                if (uc.getArea().getRegion() == null) continue;

                Integer regionId = uc.getArea().getRegionId();
                if (regionId == null) continue;
                String regionKey = String.valueOf(regionId);

                List<WeatherData> weatherList = weatherByRegion.get(regionKey);
                if (weatherList == null) continue;

                for (WeatherData wd : weatherList) {
                    if (wd.getDate() != null && wd.getDate().equals(dateStr)) {
                        double tempMin = parseDouble(wd.getTemperatureMin());
                        double tempMax = parseDouble(wd.getTemperatureMax());
                        double humMin = parseDouble(wd.getHumidityMin());
                        double windMax = parseDouble(wd.getWindMax());
                        double precipitation = parseDouble(wd.getPrecipitation());

                        PlantingRecommendation rec = new PlantingRecommendation();
                        rec.setUserCropId(uc.getId());
                        rec.setDate(displayDate);
                        rec.setDayOfWeek(dayOfWeek);
                        rec.setCropName(uc.getCrop().getName());
                        rec.setVariety(uc.getCrop().getVariety());
                        rec.setAreaName(uc.getArea().getName());
                        rec.setAreaId(uc.getAreaId());

                        Crop crop = uc.getCrop();

                        rec.setTempCurrent(String.format("%.0f-%.0f°C", tempMin, tempMax));
                        rec.setHumidityCurrent(String.format("%.0f%%", humMin));
                        rec.setPrecipCurrent(String.format("%.1f мм", precipitation));
                        rec.setWindCurrent(String.format("%.1f м/с", windMax));

                        if (crop.getMinTemp() != null && crop.getMaxTemp() != null) {
                            rec.setTempRequired(String.format("%.0f-%.0f°C", crop.getMinTemp(), crop.getMaxTemp()));
                        } else {
                            rec.setTempRequired("—");
                        }

                        if (crop.getMinHumidity() != null && crop.getMaxHumidity() != null) {
                            rec.setHumidityRequired(String.format("%.0f-%.0f%%", (float)crop.getMinHumidity(), (float)crop.getMaxHumidity()));
                        } else {
                            rec.setHumidityRequired("—");
                        }

                        if (crop.getNeededPrecipitation() != null) {
                            rec.setPrecipRequired(String.format("до %.0f мм", crop.getNeededPrecipitation()));
                        } else {
                            rec.setPrecipRequired("—");
                        }

                        if (crop.getMaxWind() != null) {
                            rec.setWindRequired(String.format("до %.0f м/с", crop.getMaxWind()));
                        } else {
                            rec.setWindRequired("—");
                        }

                        boolean tempOk = true;
                        if (crop.getMinTemp() != null) {
                            tempOk = tempOk && (tempMin >= crop.getMinTemp());
                        }
                        if (crop.getMaxTemp() != null) {
                            tempOk = tempOk && (tempMax <= crop.getMaxTemp());
                        }

                        boolean humidityOk = true;
                        if (crop.getMinHumidity() != null) {
                            humidityOk = humidityOk && (humMin >= crop.getMinHumidity());
                        }
                        if (crop.getMaxHumidity() != null) {
                            humidityOk = humidityOk && (humMin <= crop.getMaxHumidity());
                        }

                        boolean windOk = true;
                        if (crop.getMaxWind() != null) {
                            windOk = windOk && (windMax <= crop.getMaxWind());
                        }

                        boolean rainOk = true;
                        if (crop.getNeededPrecipitation() != null) {
                            rainOk = rainOk && (precipitation <= crop.getNeededPrecipitation());
                        }

                        boolean goodDay = tempOk && humidityOk && windOk && rainOk;

                        rec.setGoodDay(goodDay);
                        rec.setReason(getReasonTextFull(crop, tempMin, tempMax, humMin, precipitation, windMax));
                        rec.setWeatherText((int)tempMin + "-" + (int)tempMax + "°C");

                        recommendations.add(rec);
                        break;
                    }
                }
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        runOnUiThread(() -> {
            showLoading(false);
            if (recommendations.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                rvRecommendations.setVisibility(View.GONE);
                tvResultsTitle.setVisibility(View.GONE);
                tvEmpty.setText("Нет подходящих дней для посадки в ближайшую неделю");
            } else {
                tvEmpty.setVisibility(View.GONE);
                rvRecommendations.setVisibility(View.VISIBLE);
                tvResultsTitle.setVisibility(View.VISIBLE);
                adapter.updateData(recommendations);
            }
        });
    }

    private String getReasonTextFull(Crop crop, double tempMin, double tempMax, double hum, double precip, double wind) {
        StringBuilder sb = new StringBuilder();

        if (crop.getMinTemp() != null && crop.getMaxTemp() != null) {
            if (tempMin >= crop.getMinTemp() && tempMax <= crop.getMaxTemp()) {
                sb.append(String.format("Температура: %.0f..%.0f°C (оптимум: %.0f-%.0f°C) ✓\n",
                        tempMin, tempMax, crop.getMinTemp(), crop.getMaxTemp()));
            } else {
                sb.append(String.format("Температура: %.0f..%.0f°C (нужно: %.0f-%.0f°C) ✗\n",
                        tempMin, tempMax, crop.getMinTemp(), crop.getMaxTemp()));
            }
        } else {
            sb.append(String.format("Температура: %.0f..%.0f°C\n", tempMin, tempMax));
        }

        if (crop.getMinHumidity() != null && crop.getMaxHumidity() != null) {
            if (hum >= crop.getMinHumidity() && hum <= crop.getMaxHumidity()) {
                sb.append(String.format("Влажность: %.0f%% (оптимум: %.0f-%.0f%%) ✓\n",
                        hum, (float)crop.getMinHumidity(), (float)crop.getMaxHumidity()));
            } else {
                sb.append(String.format("Влажность: %.0f%% (нужно: %.0f-%.0f%%) ✗\n",
                        hum, (float)crop.getMinHumidity(), (float)crop.getMaxHumidity()));
            }
        } else {
            sb.append(String.format("Влажность: %.0f%%\n", hum));
        }

        if (crop.getNeededPrecipitation() != null) {
            if (precip <= crop.getNeededPrecipitation()) {
                sb.append(String.format("Осадки: %.1f мм (максимум: %.0f мм) ✓\n",
                        precip, crop.getNeededPrecipitation()));
            } else {
                sb.append(String.format("Осадки: %.1f мм (превышает норму: %.0f мм) ✗\n",
                        precip, crop.getNeededPrecipitation()));
            }
        } else {
            sb.append(String.format("Осадки: %.1f мм\n", precip));
        }

        if (crop.getMaxWind() != null) {
            if (wind <= crop.getMaxWind()) {
                sb.append(String.format("Ветер: %.1f м/с (максимум: %.0f м/с) ✓",
                        wind, crop.getMaxWind()));
            } else {
                sb.append(String.format("Ветер: %.1f м/с (превышает норму: %.0f м/с) ✗",
                        wind, crop.getMaxWind()));
            }
        } else {
            sb.append(String.format("Ветер: %.1f м/с", wind));
        }

        return sb.toString();
    }
    private void onPlantClick(PlantingRecommendation item) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Посадка")
                .setMessage("Посадить " + item.getCropName() + " на участке " + item.getAreaName() + "?")
                .setPositiveButton("Да", (d, w) -> performPlanting(item))
                .setNegativeButton("Нет", null)
                .show();
    }

    private void performPlanting(PlantingRecommendation item) {
        User currentUser = prefsHelper.getUser();
        if (currentUser == null) return;

        showLoading(true);

        Map<String, Object> request = new HashMap<>();
        request.put("userCropId", item.getUserCropId());
        request.put("areaId", item.getAreaId());
        request.put("actionTypeId", 1);

        apiService.plantCrop(request).enqueue(new retrofit2.Callback<Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<Map<String, Object>> call, retrofit2.Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PlantingRecommendationActivity.this, "Растение успешно посажено!", Toast.LENGTH_SHORT).show();
                    refreshData();
                } else {
                    showLoading(false);
                    Toast.makeText(PlantingRecommendationActivity.this, "Ошибка сервера: " + response.code(), Toast.LENGTH_LONG).show();
                    refreshData();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<Map<String, Object>> call, Throwable t) {
                showLoading(false);
                Toast.makeText(PlantingRecommendationActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            tvEmpty.setVisibility(View.GONE);
            rvRecommendations.setVisibility(View.GONE);
            tvResultsTitle.setVisibility(View.GONE);
        }
    }
}