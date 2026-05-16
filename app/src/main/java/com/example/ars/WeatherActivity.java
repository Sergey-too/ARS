package com.example.ars;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.example.ars.models.Region;
import com.example.ars.models.WeatherResponse;
import com.example.ars.models.WeatherData;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherActivity extends AppCompatActivity {

    private String selectedRegion = "Минск";
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        apiService = RetrofitClient.getApiService();

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(WeatherActivity.this, PlantsActivity.class));
            finish();
        });

        loadRegions();

        MaterialButton btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(v -> {
            loadWeatherForRegion(selectedRegion);
        });

        loadWeatherForRegion(selectedRegion);
    }

    private void loadRegions() {
        apiService.getRegions().enqueue(new Callback<List<Region>>() {
            @Override
            public void onResponse(Call<List<Region>> call, Response<List<Region>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    updateRegionDropdown(response.body());
                } else {
                    Toast.makeText(WeatherActivity.this,
                            "Регионы не найдены в базе данных", Toast.LENGTH_SHORT).show();

                    TextView tvRegionInfo = findViewById(R.id.tvRegionInfo);
                    if (tvRegionInfo != null) tvRegionInfo.setText("Регионы не загружены");
                }
            }

            @Override
            public void onFailure(Call<List<Region>> call, Throwable t) {
                Log.e("API", "Ошибка загрузки регионов: " + t.getMessage());
                Toast.makeText(WeatherActivity.this,
                        "Ошибка загрузки регионов", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRegionDropdown(List<Region> regions) {
        AutoCompleteTextView actvRegion = findViewById(R.id.actvRegion);

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
            loadWeatherForRegion(selectedRegion);
        });

        if (!regions.isEmpty()) {
            actvRegion.setText(regions.get(0).getName(), false);
            selectedRegion = regions.get(0).getName();
            updateRegionInfo();
            loadWeatherForRegion(selectedRegion);
        }
    }

    private void updateRegionInfo() {
        TextView tvRegionInfo = findViewById(R.id.tvRegionInfo);
        if (tvRegionInfo != null) tvRegionInfo.setText("Выбран регион: " + selectedRegion);
    }

    private void loadWeatherForRegion(String region) {
        apiService.getWeatherForRegion(region).enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weatherResponse = response.body();
                    updateWeatherUI(weatherResponse);

                    if (weatherResponse.isTestData()) {
                        Toast.makeText(WeatherActivity.this,
                                "Используются тестовые данные о погоде", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(WeatherActivity.this,
                            "Не удалось загрузить данные о погоде", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Log.e("API", "Ошибка загрузки погоды: " + t.getMessage());
                Toast.makeText(WeatherActivity.this,
                        "Ошибка сети при загрузке погоды", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateWeatherUI(WeatherResponse response) {
        List<WeatherData> weatherList = response.getWeather();

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String currentTime = sdf.format(new Date());

        TextView tvUpdated = findViewById(R.id.tvUpdated);
        if (tvUpdated != null) tvUpdated.setText("Данные обновлены: " + currentTime);

        if (weatherList != null) {
            for (int i = 0; i < Math.min(6, weatherList.size()); i++) {
                WeatherData weather = weatherList.get(i);
                updateWeatherRow(i + 1, weather);
            }
        }
    }

    private void updateWeatherRow(int rowNumber, WeatherData weather) {
        int dateId = getResources().getIdentifier("tvDate" + rowNumber, "id", getPackageName());
        int tempId = getResources().getIdentifier("tvTemp" + rowNumber, "id", getPackageName());
        int windId = getResources().getIdentifier("tvWind" + rowNumber, "id", getPackageName());
        int pressureId = getResources().getIdentifier("tvPressure" + rowNumber, "id", getPackageName());
        int humidityId = getResources().getIdentifier("tvHumidity" + rowNumber, "id", getPackageName());
        int precipId = getResources().getIdentifier("tvPrecipitation" + rowNumber, "id", getPackageName());

        TextView tvDate = findViewById(dateId);
        TextView tvTemp = findViewById(tempId);
        TextView tvWind = findViewById(windId);
        TextView tvPressure = findViewById(pressureId);
        TextView tvHumidity = findViewById(humidityId);
        TextView tvPrecip = findViewById(precipId);

        if (tvDate != null) {
            tvDate.setText(formatDate(weather.getDate()));
        }

        try {
            if (tvTemp != null) {
                float tMin = Float.parseFloat(weather.getTemperatureMin());
                float tMax = Float.parseFloat(weather.getTemperatureMax());
                String tempRange = String.format(Locale.getDefault(), "%.1f..%.1f°C", tMin, tMax);
                tvTemp.setText(tempRange);
            }

            if (tvWind != null) {
                float wMin = Float.parseFloat(weather.getWindMin());
                float wMax = Float.parseFloat(weather.getWindMax());
                String windRange = String.format(Locale.getDefault(), "%.1f..%.1f м/с", wMin, wMax);
                tvWind.setText(windRange);
            }

            if (tvPressure != null) {
                tvPressure.setText(weather.getPressure() + " мм");
            }

            if (humidityId != 0 && tvHumidity != null) {
                float hMin = Float.parseFloat(weather.getHumidityMin());
                float hMax = Float.parseFloat(weather.getHumidityMax());
                String humRange = String.format(Locale.getDefault(), "%.0f..%.0f%%", hMin, hMax);
                tvHumidity.setText(humRange);
            }

            if (precipId != 0 && tvPrecip != null) {
                float precipVal = Float.parseFloat(weather.getPrecipitation());
                String precip = String.format(Locale.getDefault(), "%.1f мм", precipVal);
                tvPrecip.setText(precip);
            }

        } catch (NumberFormatException e) {
            Log.e("WeatherError", "Ошибка парсинга чисел погоды: " + e.getMessage());
        }
    }

    private String formatDate(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) return "--";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date date = inputFormat.parse(rawDate);

            SimpleDateFormat outputFormat = new SimpleDateFormat("d MMMM", new Locale("ru"));
            return date != null ? outputFormat.format(date) : rawDate;
        } catch (Exception e) {
            Log.e("WeatherError", "Ошибка форматирования даты: " + rawDate);
            return rawDate;
        }
    }
}