package com.example.ars;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.Region;
import com.example.ars.models.WeatherData;
import com.example.ars.models.WeatherResponse;
import com.example.ars.utils.SharedPreferencesHelper;
import com.example.ars.utils.WeatherCacheManager;
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

public class WeatherActivity extends AppCompatActivity {

    private static final String TAG = "WeatherActivity";
    private static final String PREF_LAST_REGION = "last_selected_region";

    private String selectedRegion = "Минск";
    private WeatherCacheManager cacheManager;
    private LinearLayout container;
    private TextView tvRegionInfo;
    private TextView tvUpdated;
    private AutoCompleteTextView actvRegion;
    private List<Region> regionsList = new ArrayList<>();
    private Map<Integer, List<WeatherData>> allWeatherCache = new HashMap<>();
    private ApiService apiService;
    private SharedPreferences sharedPrefs;
    private boolean isLoadingFromServer = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        cacheManager = new WeatherCacheManager(this);
        apiService = RetrofitClient.getApiService();
        sharedPrefs = getSharedPreferences("weather_prefs", MODE_PRIVATE);

        String savedRegion = sharedPrefs.getString(PREF_LAST_REGION, "Минск");
        selectedRegion = savedRegion;

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(WeatherActivity.this, PlantsActivity.class));
            finish();
        });

        container = findViewById(R.id.llWeatherContainer);
        tvRegionInfo = findViewById(R.id.tvRegionInfo);
        tvUpdated = findViewById(R.id.tvUpdated);
        actvRegion = findViewById(R.id.actvRegion);

        MaterialButton btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                forceRefresh();
            } else {
                Toast.makeText(this, "Нет интернет-соединения", Toast.LENGTH_SHORT).show();
            }
        });

        loadData();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void loadData() {
        showLoading(true);
        if (isNetworkAvailable()) {
            loadFromServer();
        } else {
            loadFromCache();
        }
    }

    private void loadFromServer() {
        if (isLoadingFromServer) return;
        isLoadingFromServer = true;

        Log.d(TAG, "=== ЗАГРУЗКА С СЕРВЕРА ===");

        apiService.getRegions().enqueue(new Callback<List<Region>>() {
            @Override
            public void onResponse(Call<List<Region>> call, Response<List<Region>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    regionsList = response.body();
                    cacheManager.saveRegions(regionsList, null);
                    updateRegionDropdown(regionsList);
                    loadAllWeatherFromServer();
                } else {
                    isLoadingFromServer = false;
                    showLoading(false);
                    showError("Ошибка загрузки регионов");
                    loadFromCache();
                }
            }

            @Override
            public void onFailure(Call<List<Region>> call, Throwable t) {
                isLoadingFromServer = false;
                Log.e(TAG, "Ошибка сети: " + t.getMessage());
                loadFromCache();
            }
        });
    }

    private void loadAllWeatherFromServer() {
        if (regionsList.isEmpty()) {
            isLoadingFromServer = false;
            showLoading(false);
            return;
        }

        final int[] pendingRequests = {regionsList.size()};

        for (Region region : regionsList) {
            apiService.getWeatherByRegionId(region.getId()).enqueue(new Callback<WeatherResponse>() {
                @Override
                public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getWeather() != null) {
                        List<WeatherData> weatherList = response.body().getWeather();
                        allWeatherCache.put(region.getId(), weatherList);
                        cacheManager.saveForecast(region.getId(), weatherList, null);

                        if (region.getName().equals(selectedRegion)) {
                            updateWeatherUI(weatherList);
                            updateTimeStamp();
                        }
                    }

                    synchronized (pendingRequests) {
                        pendingRequests[0]--;
                        if (pendingRequests[0] == 0) {
                            isLoadingFromServer = false;
                            showLoading(false);
                            Log.d(TAG, "Все данные загружены с сервера");
                        }
                    }
                }

                @Override
                public void onFailure(Call<WeatherResponse> call, Throwable t) {
                    Log.e(TAG, "Ошибка загрузки погоды для " + region.getName());
                    synchronized (pendingRequests) {
                        pendingRequests[0]--;
                        if (pendingRequests[0] == 0) {
                            isLoadingFromServer = false;
                            showLoading(false);
                            if (region.getName().equals(selectedRegion)) {
                                loadWeatherFromCacheForSelectedRegion();
                            }
                        }
                    }
                }
            });
        }
    }

    private void loadFromCache() {
        Log.d(TAG, "=== ЗАГРУЗКА ИЗ КЭША (ОФЛАЙН) ===");

        cacheManager.getCachedRegions(new WeatherCacheManager.CacheCallback<List<Region>>() {
            @Override
            public void onSuccess(List<Region> cachedRegions) {
                if (cachedRegions != null && !cachedRegions.isEmpty()) {
                    regionsList = cachedRegions;
                    updateRegionDropdown(regionsList);
                    loadWeatherFromCacheForSelectedRegion();
                    showLoading(false);
                    Toast.makeText(WeatherActivity.this, "Офлайн-режим. Данные из кэша.", Toast.LENGTH_SHORT).show();
                } else {
                    showLoading(false);
                    showError("Нет сохраненных данных. Подключитесь к интернету.");
                }
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                showError("Нет сохраненных данных");
            }
        });
    }

    private void loadWeatherFromCacheForSelectedRegion() {
        Integer regionId = getRegionIdByName(selectedRegion);
        if (regionId != null) {
            cacheManager.getForecast(regionId, new WeatherCacheManager.CacheCallback<List<WeatherData>>() {
                @Override
                public void onSuccess(List<WeatherData> cachedWeather) {
                    if (cachedWeather != null && !cachedWeather.isEmpty()) {
                        allWeatherCache.put(regionId, cachedWeather);
                        updateWeatherUI(cachedWeather);
                        tvUpdated.setText("Данные из кэша");
                    } else {
                        showNoDataMessage();
                    }
                }

                @Override
                public void onError(String error) {
                    showError("Ошибка загрузки погоды из кэша");
                }
            });
        }
    }

    private void loadWeatherFromCacheForRegion(int regionId, String regionName) {
        cacheManager.getForecast(regionId, new WeatherCacheManager.CacheCallback<List<WeatherData>>() {
            @Override
            public void onSuccess(List<WeatherData> cachedWeather) {
                if (cachedWeather != null && !cachedWeather.isEmpty()) {
                    allWeatherCache.put(regionId, cachedWeather);
                    if (regionName.equals(selectedRegion)) {
                        updateWeatherUI(cachedWeather);
                        tvUpdated.setText("Данные из кэша");
                        showLoading(false);
                    }
                } else if (regionName.equals(selectedRegion)) {
                    showLoading(false);
                    showNoDataMessage();
                }
            }

            @Override
            public void onError(String error) {
                if (regionName.equals(selectedRegion)) {
                    showLoading(false);
                    showError("Нет данных о погоде для " + regionName);
                }
            }
        });
    }

    private void forceRefresh() {
        if (isNetworkAvailable() && !isLoadingFromServer) {
            showLoading(true);
            loadFromServer();
        }
    }

    private void updateRegionDropdown(List<Region> regions) {
        ArrayAdapter<Region> regionAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                regions
        );
        actvRegion.setAdapter(regionAdapter);

        actvRegion.setOnItemClickListener((parent, view, position, id) -> {
            Region region = (Region) parent.getItemAtPosition(position);
            selectedRegion = region.getName();
            sharedPrefs.edit().putString(PREF_LAST_REGION, selectedRegion).apply();
            updateRegionInfo();
            displayWeatherForRegion(selectedRegion);
        });

        if (!regions.isEmpty()) {
            boolean regionFound = false;
            for (Region r : regions) {
                if (r.getName().equals(selectedRegion)) {
                    actvRegion.setText(r.getName(), false);
                    regionFound = true;
                    break;
                }
            }
            if (!regionFound) {
                actvRegion.setText(regions.get(0).getName(), false);
                selectedRegion = regions.get(0).getName();
                sharedPrefs.edit().putString(PREF_LAST_REGION, selectedRegion).apply();
            }
            updateRegionInfo();
        }
    }

    private void updateRegionInfo() {
        if (tvRegionInfo != null) {
            tvRegionInfo.setText("Выбран регион: " + selectedRegion);
        }
    }

    private void updateTimeStamp() {
        if (tvUpdated != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            tvUpdated.setText("Обновлено: " + sdf.format(new Date()));
        }
    }

    private void displayWeatherForRegion(String regionName) {
        Integer regionId = getRegionIdByName(regionName);
        if (regionId == null) {
            showNoDataMessage();
            return;
        }

        List<WeatherData> weatherList = allWeatherCache.get(regionId);
        if (weatherList != null && !weatherList.isEmpty()) {
            updateWeatherUI(weatherList);
            if (isNetworkAvailable() && !isLoadingFromServer) {
                updateTimeStamp();
            }
        } else {
            loadWeatherFromCacheForRegion(regionId, regionName);
        }
    }

    private Integer getRegionIdByName(String regionName) {
        for (Region r : regionsList) {
            if (r.getName().equalsIgnoreCase(regionName)) {
                return r.getId();
            }
        }
        return null;
    }

    private void updateWeatherUI(List<WeatherData> weatherList) {
        if (container == null) return;
        container.removeAllViews();

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        for (WeatherData weather : weatherList) {
            String date = weather.getDate();
            if (date != null && date.compareTo(today) >= 0) {
                View cardView = createWeatherCard(weather);
                container.addView(cardView);
            }
        }

        if (container.getChildCount() == 0) {
            showNoDataMessage();
        }
    }

    private void showNoDataMessage() {
        if (container != null) {
            container.removeAllViews();
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("Нет данных о погоде на ближайшие дни");
            tvEmpty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tvEmpty.setPadding(50, 50, 50, 50);
            container.addView(tvEmpty);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        if (allWeatherCache.isEmpty()) {
            showNoDataMessage();
        }
    }

    private void showLoading(boolean show) {
        View progressBar = findViewById(R.id.progressBar);
        if (progressBar != null) {
               progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (show) {
            if (container != null) container.setVisibility(View.GONE);
        } else {
            if (container != null) container.setVisibility(View.VISIBLE);
        }
    }

    private View createWeatherCard(WeatherData weather) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_weather_card, null);
        TextView tvDate = card.findViewById(R.id.tvDate);
        TextView tvDayOfWeek = card.findViewById(R.id.tvDayOfWeek);
        TextView tvTemp = card.findViewById(R.id.tvTemp);
        TextView tvWind = card.findViewById(R.id.tvWind);
        TextView tvPressure = card.findViewById(R.id.tvPressure);
        TextView tvHumidity = card.findViewById(R.id.tvHumidity);
        TextView tvPrecipitation = card.findViewById(R.id.tvPrecipitation);

        String formattedDate = formatDate(weather.getDate());
        String dayOfWeek = formatDayOfWeek(weather.getDate());

        tvDate.setText(formattedDate);
        tvDayOfWeek.setText(dayOfWeek);

        double tempMin = parseDouble(weather.getTemperatureMin());
        double tempMax = parseDouble(weather.getTemperatureMax());
        tvTemp.setText(String.format(Locale.getDefault(), "%.1f...%.1f°C", tempMin, tempMax));

        double windMin = parseDouble(weather.getWindMin());
        double windMax = parseDouble(weather.getWindMax());
        tvWind.setText(String.format(Locale.getDefault(), "%.1f...%.1f м/с", windMin, windMax));

        String pressure = weather.getPressure() != null ? weather.getPressure() : "--";
        tvPressure.setText(pressure + " гПа");

        double humMin = parseDouble(weather.getHumidityMin());
        double humMax = parseDouble(weather.getHumidityMax());
        tvHumidity.setText(String.format(Locale.getDefault(), "%.0f...%.0f%%", humMin, humMax));

        double precipitation = parseDouble(weather.getPrecipitation());
        tvPrecipitation.setText(String.format(Locale.getDefault(), "%.1f мм", precipitation));

        return card;
    }

    private String formatDate(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) return "--";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("d MMMM", new Locale("ru"));
            Date date = inputFormat.parse(rawDate);
            return date != null ? outputFormat.format(date) : rawDate;
        } catch (Exception e) {
            return rawDate;
        }
    }

    private String formatDayOfWeek(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) return "";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("ru"));
            Date date = inputFormat.parse(rawDate);
            return date != null ? dayFormat.format(date) : "";
        } catch (Exception e) {
            return "";
        }
    }

    private double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}