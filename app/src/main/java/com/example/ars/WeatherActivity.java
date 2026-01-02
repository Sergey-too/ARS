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

        // Кнопка назад
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(WeatherActivity.this, PlantsActivity.class));
            finish();
        });

        // Загружаем регионы ТОЛЬКО из БД
        loadRegions();

        // Кнопка обновления
        MaterialButton btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(v -> {
            loadWeatherForRegion(selectedRegion);
        });

        // Загружаем погоду для выбранного региона
        loadWeatherForRegion(selectedRegion);
    }

    private void loadRegions() {
        apiService.getRegions().enqueue(new Callback<List<Region>>() {
            @Override
            public void onResponse(Call<List<Region>> call, Response<List<Region>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    updateRegionDropdown(response.body());
                } else {
                    // Если регионов нет в БД, сообщаем пользователю
                    Toast.makeText(WeatherActivity.this,
                            "Регионы не найдены в базе данных", Toast.LENGTH_SHORT).show();
                    Log.e("API", "Регионы не найдены в БД");

                    // Скрываем или показываем сообщение
                    TextView tvRegionInfo = findViewById(R.id.tvRegionInfo);
                    tvRegionInfo.setText("Регионы не загружены");
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

        // УДАЛИЛ статичные регионы - используем только из БД
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

        // Устанавливаем начальное значение из БД
        if (!regions.isEmpty()) {
            actvRegion.setText(regions.get(0).getName(), false);
            selectedRegion = regions.get(0).getName();
            updateRegionInfo();
            // Автоматически грузим погоду для первого региона
            loadWeatherForRegion(selectedRegion);
        }
    }

    // УДАЛИЛ метод setupRegionDropdown() - он был с тестовыми данными

    private void updateRegionInfo() {
        TextView tvRegionInfo = findViewById(R.id.tvRegionInfo);
        tvRegionInfo.setText("Выбран регион: " + selectedRegion);
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

        // Обновляем дату последнего обновления
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String currentTime = sdf.format(new Date());

        TextView tvUpdated = findViewById(R.id.tvUpdated);
        tvUpdated.setText("Данные обновлены: " + currentTime);

        // Обновляем данные в таблице (первые 6 записей)
        for (int i = 0; i < Math.min(6, weatherList.size()); i++) {
            WeatherData weather = weatherList.get(i);
            updateWeatherRow(i + 1, weather);
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

        if (tvDate != null) tvDate.setText(weather.getDate());
        if (tvTemp != null) tvTemp.setText(weather.getTemperature());
        if (tvWind != null) tvWind.setText(weather.getWind());
        if (tvPressure != null) tvPressure.setText(weather.getPressure());
        if (tvHumidity != null) tvHumidity.setText(weather.getHumidity());
        if (tvPrecip != null) tvPrecip.setText(weather.getPrecipitation());
    }

    // УДАЛИЛ метод addRealWeatherData() - если нужны тестовые данные, их должен создавать сервер
}