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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.adapters.WeatherAdapter;
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
    private WeatherAdapter weatherAdapter;

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

        weatherAdapter = new WeatherAdapter();

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

        if (weatherList != null && !weatherList.isEmpty()) {
            LinearLayout container = findViewById(R.id.llWeatherContainer);
            container.removeAllViews();

            for (WeatherData weather : weatherList) {
                View cardView = createWeatherCard(weather);
                container.addView(cardView);
            }
        } else {
            Toast.makeText(this, "Нет данных о погоде", Toast.LENGTH_SHORT).show();
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