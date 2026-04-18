package com.example.ars;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.Region;
import com.example.ars.models.WeatherComparisonDTO;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherStatsActivity extends AppCompatActivity {

    private ApiService apiService;
    private LineChart tempChart, humChart;
    private AutoCompleteTextView actvRegion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_stats);

        // Инициализация UI
        apiService = RetrofitClient.getApiService();
        tempChart = findViewById(R.id.tempBarChart);
        humChart = findViewById(R.id.humBarChart);
        actvRegion = findViewById(R.id.actvRegionStats);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Загрузка регионов и установка Минска
        loadRegions();

        // Обработка выбора региона
        actvRegion.setOnItemClickListener((parent, view, position, id) -> {
            Region selectedRegion = (Region) parent.getAdapter().getItem(position);
            if (selectedRegion != null) {
                loadComparisonData(selectedRegion.getId());
                Toast.makeText(this, "Загрузка: " + selectedRegion.getName(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRegions() {
        apiService.getAllRegions().enqueue(new Callback<List<Region>>() {
            @Override
            public void onResponse(Call<List<Region>> call, Response<List<Region>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Region> regions = response.body();
                    ArrayAdapter<Region> adapter = new ArrayAdapter<>(
                            WeatherStatsActivity.this,
                            android.R.layout.simple_dropdown_item_1line,
                            regions
                    );
                    actvRegion.setAdapter(adapter);

                    for (Region r : regions) {
                        if (r.getId() == 1L) {
                            actvRegion.setText(r.getName(), false);
                            loadComparisonData(r.getId());
                            break;
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Region>> call, Throwable t) {
                Log.e("Stats", "Ошибка загрузки регионов: " + t.getMessage());
            }
        });
    }

    private void loadComparisonData(Long regionId) {
        apiService.getWeatherComparison(regionId).enqueue(new Callback<List<WeatherComparisonDTO>>() {
            @Override
            public void onResponse(Call<List<WeatherComparisonDTO>> call, Response<List<WeatherComparisonDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateCharts(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<WeatherComparisonDTO>> call, Throwable t) {
                Toast.makeText(WeatherStatsActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCharts(List<WeatherComparisonDTO> dataList) {
        for (WeatherComparisonDTO d : dataList) {
            Log.d("DEBUG_DATA", "Месяц: " + d.getMonthName() +
                    " | Факт Темп: " + d.getAvgFactTemp() +
                    " | Факт Влажн: " + d.getAvgFactHumidity());
        }
        ArrayList<Entry> tempFactEntries = new ArrayList<>();
        ArrayList<Entry> tempNormEntries = new ArrayList<>();
        ArrayList<Entry> humFactEntries = new ArrayList<>();
        ArrayList<Entry> humNormEntries = new ArrayList<>();

        final String[] monthLabels = new String[dataList.size()];

        for (int i = 0; i < dataList.size(); i++) {
            WeatherComparisonDTO d = dataList.get(i);

            tempFactEntries.add(new Entry(i, d.getAvgFactTemp().floatValue()));
            tempNormEntries.add(new Entry(i, d.getNormalTemp().floatValue()));

            humFactEntries.add(new Entry(i, d.getAvgFactHumidity().floatValue()));
            humNormEntries.add(new Entry(i, d.getNormalHumidity().floatValue()));

            monthLabels[i] = d.getMonthName();
        }

        renderLineChart(tempChart, tempFactEntries, tempNormEntries, "Температура (°C)", monthLabels, Color.RED);
        renderLineChart(humChart, humFactEntries, humNormEntries, "Влажность (%)", monthLabels, Color.BLUE);
    }

    private void renderLineChart(LineChart chart, ArrayList<Entry> fact, ArrayList<Entry> norm, String label, String[] labels, int color) {
        // 1. ПОЛНАЯ ОЧИСТКА (Критично для обновления данных)
        chart.clear();

        LineDataSet factSet = new LineDataSet(fact, "Факт");
        factSet.setColor(color);
        factSet.setCircleColor(color);
        factSet.setLineWidth(3f);
        factSet.setCircleRadius(4f);
        factSet.setDrawValues(false);

        LineDataSet normSet = new LineDataSet(norm, "Норма");
        normSet.setColor(Color.GRAY);
        normSet.setCircleColor(Color.GRAY);
        normSet.setLineWidth(2f);
        normSet.enableDashedLine(10f, 5f, 0f);
        normSet.setDrawValues(false);

        LineData lineData = new LineData(factSet, normSet);
        chart.setData(lineData);

        // 2. НАСТРОЙКА ОСИ X (Используем переданный в метод chart)
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        // Важно: обновляем форматтер меток под новые данные
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setYOffset(5f);

        // 3. НАСТРОЙКА ЛЕГЕНДЫ
        Legend legend = chart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setYOffset(10f);
        legend.setForm(Legend.LegendForm.CIRCLE);

        // Добавляем отступ, чтобы легенда не "съедалась"
        chart.setExtraBottomOffset(25f);

        chart.getAxisRight().setEnabled(false);
        chart.getDescription().setEnabled(false);

        // 4. ПРИНУДИТЕЛЬНОЕ ОБНОВЛЕНИЕ
        chart.notifyDataSetChanged(); // Сообщить, что данные изменились
        chart.animateX(800);
        chart.invalidate(); // Перерисовать
    }
}