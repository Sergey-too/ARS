package com.example.ars;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class WeatherActivity extends AppCompatActivity {

    private String selectedRegion = "Минская область";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // Кнопка назад
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Настройка выпадающего списка регионов
        setupRegionDropdown();

        // Кнопка обновления
        MaterialButton btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(v -> {
            updateWeatherData();
        });

        // Устанавливаем даты (последние 6 дней)
        setDates();

        // Обновляем информацию о регионе
        updateRegionInfo();
    }

    private void setupRegionDropdown() {
        // Областные города Беларуси
        String[] regions = {
                "Минск, Минская область",
                "Брестская область",
                "Витебская область",
                "Гомельская область",
                "Гродненская область",
                "Могилевская область"
        };

        AutoCompleteTextView actvRegion = findViewById(R.id.actvRegion);
        ArrayAdapter<String> regionAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                regions
        );
        actvRegion.setAdapter(regionAdapter);

        // Устанавливаем обработчик выбора региона
        actvRegion.setOnItemClickListener((parent, view, position, id) -> {
            selectedRegion = regions[position];
            updateRegionInfo();
            // Здесь можно добавить загрузку данных для выбранного региона
            loadWeatherForRegion(selectedRegion);
        });

        // Устанавливаем начальное значение
        actvRegion.setText(regions[0], false);
    }

    private void updateRegionInfo() {
        TextView tvRegionInfo = findViewById(R.id.tvRegionInfo);
        tvRegionInfo.setText("Выбран: " + selectedRegion);
    }

    private void loadWeatherForRegion(String region) {
        // Здесь будет загрузка данных для выбранного региона
        Toast.makeText(this, "Загрузка данных для " + region, Toast.LENGTH_SHORT).show();

        // Обновляем данные в таблице (заглушка)
        updateWeatherData();
    }

    private void setDates() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        // Устанавливаем даты для 6 строк
        TextView[] dateViews = {
                findViewById(R.id.tvDate1),
                findViewById(R.id.tvDate2),
                findViewById(R.id.tvDate3),
                findViewById(R.id.tvDate4),
                findViewById(R.id.tvDate5),
                findViewById(R.id.tvDate6)
        };

        for (int i = 0; i < 6; i++) {
            dateViews[i].setText(sdf.format(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_YEAR, -1); // предыдущий день
        }
    }

    private void updateWeatherData() {
        // Здесь будет запрос к API погоды
        // Пока просто обновим время последнего обновления
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String currentTime = sdf.format(Calendar.getInstance().getTime());

        TextView tvUpdated = findViewById(R.id.tvUpdated);
        tvUpdated.setText("Данные обновлены: " + currentTime);

        // Можно добавить анимацию
        Toast.makeText(this, "Данные для " + selectedRegion + " обновлены", Toast.LENGTH_SHORT).show();
    }
}