package com.example.ars;

import android.content.Intent;
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
import com.example.ars.utils.WeatherCacheManager;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeatherActivity extends AppCompatActivity {

    private String selectedRegion = "Минск";
    private WeatherCacheManager cacheManager;
    private LinearLayout container;
    private TextView tvRegionInfo;
    private TextView tvUpdated;
    private AutoCompleteTextView actvRegion;
    private List<Region> regionsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        cacheManager = new WeatherCacheManager(this);

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
            // Обновить из кэша (перезагрузить)
            loadFromCache();
        });

        loadFromCache();
    }

    private void loadFromCache() {
        cacheManager.hasCachedRegions(new WeatherCacheManager.CacheCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean hasCache) {
                if (hasCache) {
                    cacheManager.getCachedRegions(new WeatherCacheManager.CacheCallback<List<Region>>() {
                        @Override
                        public void onSuccess(List<Region> cachedRegions) {
                            if (cachedRegions != null && !cachedRegions.isEmpty()) {
                                regionsList = cachedRegions;
                                updateRegionDropdown(regionsList);
                                loadWeatherFromCache(selectedRegion);
                            } else {
                                showNoDataMessage();
                            }
                        }

                        @Override
                        public void onError(String error) {
                            showNoDataMessage();
                        }
                    });
                } else {
                    showNoDataMessage();
                    Toast.makeText(WeatherActivity.this,
                            "Данные еще не загружены. Подключитесь к интернету и перезапустите приложение.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String error) {
                showNoDataMessage();
            }
        });
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
            updateRegionInfo();
            loadWeatherFromCache(selectedRegion);
        });

        if (!regions.isEmpty()) {
            actvRegion.setText(regions.get(0).getName(), false);
            selectedRegion = regions.get(0).getName();
            updateRegionInfo();
        }
    }

    private void updateRegionInfo() {
        if (tvRegionInfo != null) {
            tvRegionInfo.setText("Выбран регион: " + selectedRegion);
        }
    }

    private void loadWeatherFromCache(String region) {
        Integer regionId = getRegionIdByName(region);
        if (regionId == null) {
            showNoDataMessage();
            return;
        }

        cacheManager.getForecast(regionId, new WeatherCacheManager.CacheCallback<List<WeatherData>>() {
            @Override
            public void onSuccess(List<WeatherData> cachedWeather) {
                if (cachedWeather != null && !cachedWeather.isEmpty()) {
                    updateWeatherUI(cachedWeather);
                    if (tvUpdated != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                        tvUpdated.setText("Данные из кэша: " + sdf.format(new Date()));
                    }
                } else {
                    showNoDataMessage();
                }
            }

            @Override
            public void onError(String error) {
                showNoDataMessage();
            }
        });
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
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 6);
        String sixDaysLater = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());

        int displayedCount = 0;
        for (WeatherData weather : weatherList) {
            String date = weather.getDate();
            if (date != null && date.compareTo(today) >= 0 && date.compareTo(sixDaysLater) <= 0) {
                View cardView = createWeatherCard(weather);
                container.addView(cardView);
                displayedCount++;
            }
        }

        if (displayedCount == 0) {
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