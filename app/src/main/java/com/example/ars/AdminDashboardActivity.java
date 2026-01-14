package com.example.ars;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.Category;
import com.example.ars.models.Crop;
import com.example.ars.models.Region;
import com.example.ars.models.WeatherData;
import com.example.ars.models.WeatherResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboardActivity";
    private ApiService apiService;
    private LinearLayout containerPlantsRows;
    private LinearLayout containerWeatherRows;
    private TextView tvPlantsCount;
    private TextView tvWeatherInfo;
    private TextView tvNoPlants;
    private TextView tvNoWeather;
    private TextView tvPlantsStats;
    private TextView tvWeatherStats;
    private TextView tvAdminInfo;

    // Списки данных
    private List<Crop> allPlants = new ArrayList<>();
    private Map<String, String> plantCategories = new HashMap<>();
    private Map<String, List<WeatherData>> regionWeatherMap = new HashMap<>();
    private List<Region> allRegions = new ArrayList<>();

    @SuppressLint({"WrongViewCast", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        apiService = RetrofitClient.getApiService();

        // Инициализация элементов
        containerPlantsRows = findViewById(R.id.containerPlantsRows);
        containerWeatherRows = findViewById(R.id.containerWeatherRows);
        tvPlantsCount = findViewById(R.id.tvPlantsCount);
        tvWeatherInfo = findViewById(R.id.tvWeatherInfo);
        tvNoPlants = findViewById(R.id.tvNoPlants);
        tvNoWeather = findViewById(R.id.tvNoWeather);
        tvPlantsStats = findViewById(R.id.tvPlantsStats);
        tvWeatherStats = findViewById(R.id.tvWeatherStats);
        tvAdminInfo = findViewById(R.id.tvAdminInfo);

        // Устанавливаем время обновления
        updateAdminInfo();

        // Кнопки растений
        MaterialButton btnAddPlant = findViewById(R.id.btnAddPlant);
        btnAddPlant.setOnClickListener(v -> showAddPlantDialog());

        MaterialButton btnRefreshPlants = findViewById(R.id.btnRefreshPlants);
        btnRefreshPlants.setOnClickListener(v -> {
            containerPlantsRows.removeAllViews();
            loadAllPlants();
        });

        // Кнопки погоды
        MaterialButton btnRefreshWeather = findViewById(R.id.btnRefreshWeather);
        btnRefreshWeather.setOnClickListener(v -> {
            containerWeatherRows.removeAllViews();
            loadAllWeatherData();
        });

        // Загружаем данные
        loadAllPlants();
        loadAllWeatherData();
    }

    private void updateAdminInfo() {
        if (tvAdminInfo != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            String currentTime = sdf.format(new Date());
            tvAdminInfo.setText("Управление данными системы | " + currentTime);
        }
    }

    private void loadAllPlants() {
        tvNoPlants.setVisibility(View.GONE);
        allPlants.clear();
        plantCategories.clear();

        // Сначала загружаем категории
        apiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Category> categories = response.body();
                    if (categories.isEmpty()) {
                        showNoPlants("Категории не найдены");
                        return;
                    }
                    loadAllPlantsByCategories(categories, 0);
                } else {
                    showNoPlants("Ошибка загрузки категорий: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Log.e(TAG, "Ошибка загрузки категорий", t);
                showNoPlants("Ошибка сети: " + t.getMessage());
            }
        });
    }

    private void loadAllPlantsByCategories(List<Category> categories, int index) {
        if (index >= categories.size()) {
            // Все категории загружены
            updatePlantsUI();
            return;
        }

        Category category = categories.get(index);
        apiService.getCropsByCategory(category.getName()).enqueue(new Callback<List<Crop>>() {
            @Override
            public void onResponse(Call<List<Crop>> call, Response<List<Crop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Crop> plants = response.body();
                    allPlants.addAll(plants);

                    // Сохраняем категории для растений
                    for (Crop plant : plants) {
                        plantCategories.put(String.valueOf(plant.getId()), category.getName());
                    }
                }

                // Загружаем следующую категорию
                loadAllPlantsByCategories(categories, index + 1);
            }

            @Override
            public void onFailure(Call<List<Crop>> call, Throwable t) {
                Log.e(TAG, "Ошибка загрузки растений категории " + category.getName(), t);
                loadAllPlantsByCategories(categories, index + 1);
            }
        });
    }

    private void updatePlantsUI() {
        if (allPlants.isEmpty()) {
            showNoPlants("Растения не найдены");
            return;
        }

        // Обновляем статистику
        tvPlantsCount.setText("Всего растений: " + allPlants.size());

        // Подсчет по категориям
        Map<String, Integer> categoryCount = new HashMap<>();
        for (Crop plant : allPlants) {
            String catName = plantCategories.get(String.valueOf(plant.getId()));
            if (catName != null) {
                categoryCount.put(catName, categoryCount.getOrDefault(catName, 0) + 1);
            }
        }

        // Формируем текст статистики
        StringBuilder stats = new StringBuilder("Категории: ");
        int count = 0;
        for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
            if (count > 0) stats.append(" • ");
            stats.append(entry.getKey()).append(": ").append(entry.getValue());
            count++;
        }

        if (tvPlantsStats != null) {
            tvPlantsStats.setText(stats.toString());
        }

        // Отображаем растения в таблице
        for (Crop plant : allPlants) {
            addPlantRow(plant);
        }
    }

    private void addPlantRow(Crop plant) {
        if (containerPlantsRows == null) return;

        View rowView = LayoutInflater.from(this).inflate(R.layout.item_admin_plant_row, containerPlantsRows, false);

        TextView tvId = rowView.findViewById(R.id.tvPlantId);
        TextView tvName = rowView.findViewById(R.id.tvPlantName);
        TextView tvCategory = rowView.findViewById(R.id.tvPlantCategory);
        TextView tvTempMin = rowView.findViewById(R.id.tvPlantTempMin);
        TextView tvTempMax = rowView.findViewById(R.id.tvPlantTempMax);
        TextView tvHumidity = rowView.findViewById(R.id.tvPlantHumidity);
        TextView tvWind = rowView.findViewById(R.id.tvPlantWind);
        TextView tvPrecipitation = rowView.findViewById(R.id.tvPlantPrecipitation);
        TextView tvSowingDepth = rowView.findViewById(R.id.tvPlantSowingDepth);
        MaterialButton btnEdit = rowView.findViewById(R.id.btnEditPlant);
        MaterialButton btnDelete = rowView.findViewById(R.id.btnDeletePlant);

        // Заполняем данные
        tvId.setText(String.valueOf(plant.getId()));
        tvName.setText(plant.getName() != null ? plant.getName() : "-");

        String categoryName = plantCategories.get(String.valueOf(plant.getId()));
        tvCategory.setText(categoryName != null ? categoryName : "-");

        // Температура мин
        tvTempMin.setText(plant.getMinTemp() != null ? plant.getMinTemp() + "°C" : "-");

        // Температура макс
        tvTempMax.setText(plant.getMaxTemp() != null ? plant.getMaxTemp() + "°C" : "-");

        // Влажность
        if (plant.getMinHumidity() != null && plant.getMaxHumidity() != null) {
            tvHumidity.setText(plant.getMinHumidity() + "-" + plant.getMaxHumidity() + "%");
        } else if (plant.getMinHumidity() != null) {
            tvHumidity.setText("от " + plant.getMinHumidity() + "%");
        } else if (plant.getMaxHumidity() != null) {
            tvHumidity.setText("до " + plant.getMaxHumidity() + "%");
        } else {
            tvHumidity.setText("-");
        }

        // Ветер
        tvWind.setText(plant.getMaxWind() != null ? plant.getMaxWind() + " м/с" : "-");

        // Осадки
        tvPrecipitation.setText(plant.getNeededPrecipitation() != null ? plant.getNeededPrecipitation() + " мм" : "-");

        // Глубина посева
        tvSowingDepth.setText(plant.getSowingDepth() != null ? plant.getSowingDepth() + " см" : "-");

        // Кнопки действий
        btnEdit.setOnClickListener(v -> showEditPlantDialog(plant));
        btnDelete.setOnClickListener(v -> showDeletePlantDialog(plant));

        containerPlantsRows.addView(rowView);

        // Добавляем разделитель
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(getResources().getColor(R.color.color_divider));
        containerPlantsRows.addView(divider);
    }

    private void loadAllWeatherData() {
        tvNoWeather.setVisibility(View.GONE);
        containerWeatherRows.removeAllViews();
        regionWeatherMap.clear();
        allRegions.clear();

        // Загружаем регионы
        apiService.getRegions().enqueue(new Callback<List<Region>>() {
            @Override
            public void onResponse(Call<List<Region>> call, Response<List<Region>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allRegions = response.body();
                    if (allRegions.isEmpty()) {
                        showNoWeather("Регионы не найдены в БД");
                        return;
                    }
                    loadWeatherForAllRegions(0);
                } else {
                    showNoWeather("Ошибка загрузки регионов: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Region>> call, Throwable t) {
                Log.e(TAG, "Ошибка загрузки регионов", t);
                showNoWeather("Ошибка сети: " + t.getMessage());
            }
        });
    }

    private void loadWeatherForAllRegions(int index) {
        if (index >= allRegions.size()) {
            // Все регионы загружены
            updateWeatherUI();
            return;
        }

        Region region = allRegions.get(index);
        final String regionName = region.getName();

        apiService.getWeatherForRegion(regionName).enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weatherResponse = response.body();
                    List<WeatherData> weatherList = weatherResponse.getWeather();

                    if (weatherList != null && !weatherList.isEmpty()) {
                        // Сохраняем данные с привязкой к региону
                        regionWeatherMap.put(regionName, weatherList);

                        // Если тестовые данные, показываем сообщение
                        if (weatherResponse.isTestData()) {
                            Toast.makeText(AdminDashboardActivity.this,
                                    "Для региона " + regionName + " используются тестовые данные",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Log.e(TAG, "Ошибка загрузки погоды для региона: " + regionName + ", код: " + response.code());
                }

                // Загружаем следующий регион
                loadWeatherForAllRegions(index + 1);
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Log.e(TAG, "Ошибка сети для региона: " + regionName, t);
                loadWeatherForAllRegions(index + 1);
            }
        });
    }

    private void updateWeatherUI() {
        // Подсчитываем общее количество записей
        int totalRecords = 0;
        for (List<WeatherData> weatherList : regionWeatherMap.values()) {
            totalRecords += weatherList.size();
        }

        if (totalRecords == 0) {
            showNoWeather("Погодные данные не найдены");
            return;
        }

        // Обновляем статистику
        tvWeatherInfo.setText("Всего записей: " + totalRecords);

        if (tvWeatherStats != null) {
            tvWeatherStats.setText("Регионов: " + allRegions.size() + " | Данные: " + totalRecords + " зап.");
        }

        // Отображаем все записи
        int rowNumber = 1;
        for (Map.Entry<String, List<WeatherData>> entry : regionWeatherMap.entrySet()) {
            String regionName = entry.getKey();
            List<WeatherData> weatherList = entry.getValue();

            for (WeatherData weather : weatherList) {
                addWeatherRow(weather, regionName, rowNumber++);
            }
        }
    }

    private void addWeatherRow(WeatherData weather, String regionName, int rowNumber) {
        if (containerWeatherRows == null) return;

        View rowView = LayoutInflater.from(this).inflate(R.layout.item_admin_weather_row, containerWeatherRows, false);

        TextView tvId = rowView.findViewById(R.id.tvWeatherId);
        TextView tvRegion = rowView.findViewById(R.id.tvWeatherRegion);
        TextView tvDate = rowView.findViewById(R.id.tvWeatherDate);
        TextView tvTemp = rowView.findViewById(R.id.tvWeatherTemp);
        TextView tvWind = rowView.findViewById(R.id.tvWeatherWind);
        TextView tvPressure = rowView.findViewById(R.id.tvWeatherPressure);
        TextView tvHumidity = rowView.findViewById(R.id.tvWeatherHumidity);
        TextView tvPrecipitation = rowView.findViewById(R.id.tvWeatherPrecipitation);
        MaterialButton btnDelete = rowView.findViewById(R.id.btnDeleteWeather);

        // Заполняем данные
        tvId.setText(String.valueOf(rowNumber));
        tvRegion.setText(regionName != null ? regionName : "-");
        tvDate.setText(weather.getDate() != null ? weather.getDate() : "-");
        tvTemp.setText(weather.getTemperature() != null ? weather.getTemperature() : "-");
        tvWind.setText(weather.getWind() != null ? weather.getWind() : "-");
        tvPressure.setText(weather.getPressure() != null ? weather.getPressure() + " гПа" : "-");
        tvHumidity.setText(weather.getHumidity() != null ? weather.getHumidity() : "-");
        tvPrecipitation.setText(weather.getPrecipitation() != null ? weather.getPrecipitation() : "-");

        // Кнопки действий
        btnDelete.setOnClickListener(v -> showDeleteWeatherDialog(weather, regionName));

        containerWeatherRows.addView(rowView);

        // Добавляем разделитель
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(getResources().getColor(R.color.color_divider));
        containerWeatherRows.addView(divider);
    }

    // Диалоговые окна
    private void showAddPlantDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Добавить растение")
                .setMessage("Функция добавления растений будет реализована в следующем обновлении")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showEditPlantDialog(Crop plant) {
        String category = plantCategories.get(String.valueOf(plant.getId()));
        String description = plant.getDescription();

        new MaterialAlertDialogBuilder(this)
                .setTitle("Редактировать растение")
                .setMessage("Редактирование растения:\n\n" +
                        "📌 Название: " + plant.getName() + "\n" +
                        "🏷️ ID: " + plant.getId() + "\n" +
                        "📁 Категория: " + (category != null ? category : "Неизвестно") + "\n" +
                        "📝 Описание: " + (description != null && !description.isEmpty() ?
                        description.substring(0, Math.min(100, description.length())) + "..." : "Нет описания") + "\n" +
                        "🌡️ Температура: " +
                        (plant.getMinTemp() != null ? plant.getMinTemp() + "°C" : "-") + " / " +
                        (plant.getMaxTemp() != null ? plant.getMaxTemp() + "°C" : "-") + "\n" +
                        "💧 Влажность: " +
                        (plant.getMinHumidity() != null ? plant.getMinHumidity() + "%" : "-") + " / " +
                        (plant.getMaxHumidity() != null ? plant.getMaxHumidity() + "%" : "-"))
                .setPositiveButton("Редактировать", (dialog, which) -> {
                    Toast.makeText(this, "Функция редактирования в разработке", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showDeletePlantDialog(Crop plant) {
        String category = plantCategories.get(String.valueOf(plant.getId()));

        new MaterialAlertDialogBuilder(this)
                .setTitle("Удалить растение")
                .setMessage("Вы уверены, что хотите удалить растение?\n\n" +
                        "❌ Название: " + plant.getName() + "\n" +
                        "🏷️ ID: " + plant.getId() + "\n" +
                        "📁 Категория: " + (category != null ? category : "Неизвестно") + "\n\n" +
                        "⚠️ Это действие нельзя отменить!")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    Toast.makeText(this,
                            "Удаление растения \"" + plant.getName() + "\" (ID: " + plant.getId() + ") в разработке",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showDeleteWeatherDialog(WeatherData weather, String regionName) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Удалить данные о погоде")
                .setMessage("Вы уверены, что хотите удалить данные?\n\n" +
                        "🗺️ Регион: " + regionName + "\n" +
                        "📅 Дата: " + weather.getDate() + "\n" +
                        "🌡️ Температура: " + weather.getTemperature() + "\n" +
                        "💧 Влажность: " + weather.getHumidity() + "\n" +
                        "💨 Ветер: " + weather.getWind() + "\n\n" +
                        "⚠️ Это действие нельзя отменить!")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    Toast.makeText(this,
                            "Удаление данных за " + weather.getDate() + " (" + regionName + ") в разработке",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showNoPlants(String message) {
        tvNoPlants.setText(message);
        tvNoPlants.setVisibility(View.VISIBLE);
        tvPlantsCount.setText("Всего растений: 0");
        if (tvPlantsStats != null) {
            tvPlantsStats.setText("Нет данных");
        }
    }

    private void showNoWeather(String message) {
        tvNoWeather.setText(message);
        tvNoWeather.setVisibility(View.VISIBLE);
        tvWeatherInfo.setText("Всего записей: 0");
        if (tvWeatherStats != null) {
            tvWeatherStats.setText("Нет данных");
        }
    }
}