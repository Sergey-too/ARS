package com.example.ars;

import static com.example.ars.api.RetrofitClient.prefsHelper;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.Category;
import com.example.ars.models.Crop;
import com.example.ars.models.DeleteResponse;
import com.example.ars.models.Region;
import com.example.ars.models.WeatherData;
import com.example.ars.models.WeatherResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

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
    private AutoCompleteTextView actvRegionFilter;
    private TextInputLayout tilRegionFilter;

    // Списки данных
    private List<Crop> allPlants = new ArrayList<>();
    private Map<String, String> plantCategories = new HashMap<>();
    private List<Region> allRegions = new ArrayList<>();
    private List<WeatherData> selectedRegionWeather = new ArrayList<>();
    private String selectedRegion = "";

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

        // Элементы фильтрации по региону
        tilRegionFilter = findViewById(R.id.tilRegionFilter);
        actvRegionFilter = findViewById(R.id.actvRegionFilter);

        // Устанавливаем время обновления
        updateAdminInfo();

        // Кнопки растений
        Button btnAddPlant = findViewById(R.id.btnAddPlant);
        btnAddPlant.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this,
                    AddPlantActivityAdmin.class);
            startActivity(intent);
        });

        MaterialButton btnRefreshPlants = findViewById(R.id.btnRefreshPlants);
        btnRefreshPlants.setOnClickListener(v -> {
            containerPlantsRows.removeAllViews();
            loadAllPlants();
        });

        ImageView btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> logout());

        // Кнопки погоды
        MaterialButton btnRefreshWeather = findViewById(R.id.btnRefreshWeather);
        btnRefreshWeather.setOnClickListener(v -> {
            if (selectedRegion.isEmpty()) {
                Toast.makeText(this, "Сначала выберите регион", Toast.LENGTH_SHORT).show();
                return;
            }
            loadWeatherForSelectedRegion();
        });

        // Загружаем растения
        loadAllPlants();

        // Загружаем регионы для выпадающего списка
        loadRegionsForFilter();
    }

    private void updateAdminInfo() {
        if (tvAdminInfo != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            String currentTime = sdf.format(new Date());
            tvAdminInfo.setText("Управление данными системы | " + currentTime);
        }
    }
    private void logout() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Выход")
                .setMessage("Вы действительно хотите выйти из панели администратора?")
                .setPositiveButton("Выйти", (dialog, which) -> performLogout())
                .setNegativeButton("Отмена", null)
                .show();
    }
    private void performLogout() {
        prefsHelper.clearAll();
        Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    private void loadRegionsForFilter() {
        apiService.getRegions().enqueue(new Callback<List<Region>>() {
            @Override
            public void onResponse(Call<List<Region>> call, Response<List<Region>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allRegions = response.body();
                    if (allRegions.isEmpty()) {
                        tvWeatherStats.setText("Регионы не найдены");
                        return;
                    }

                    // Настраиваем выпадающий список
                    ArrayAdapter<Region> regionAdapter = new ArrayAdapter<>(
                            AdminDashboardActivity.this,
                            android.R.layout.simple_dropdown_item_1line,
                            allRegions
                    );
                    actvRegionFilter.setAdapter(regionAdapter);

                    // Обработчик выбора региона
                    actvRegionFilter.setOnItemClickListener((parent, view, position, id) -> {
                        Region region = (Region) parent.getItemAtPosition(position);
                        selectedRegion = region.getName();
                        tvWeatherStats.setText("Выбран: " + selectedRegion);

                        // Загружаем погоду для выбранного региона
                        loadWeatherForSelectedRegion();
                    });

                    // Устанавливаем первый регион по умолчанию
                    if (!allRegions.isEmpty()) {
                        selectedRegion = allRegions.get(0).getName();
                        actvRegionFilter.setText(selectedRegion, false);
                        tvWeatherStats.setText("Выбран: " + selectedRegion);

                        // Загружаем погоду для первого региона
                        loadWeatherForSelectedRegion();
                    }
                } else {
                    tvWeatherStats.setText("Ошибка загрузки регионов: " + response.code());
                    Log.e(TAG, "Ошибка загрузки регионов: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Region>> call, Throwable t) {
                Log.e(TAG, "Ошибка загрузки регионов", t);
                tvWeatherStats.setText("Ошибка сети: " + t.getMessage());
            }
        });
    }

    private void loadWeatherForSelectedRegion() {
        if (selectedRegion.isEmpty()) {
            Toast.makeText(this, "Выберите регион", Toast.LENGTH_SHORT).show();
            return;
        }

        tvNoWeather.setVisibility(View.GONE);
        containerWeatherRows.removeAllViews();
        selectedRegionWeather.clear();

        tvWeatherInfo.setText("Загрузка данных для: " + selectedRegion);

        // Используем НОВЫЙ эндпоинт для получения ВСЕХ записей
        apiService.getAllWeatherForRegion(selectedRegion).enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weatherResponse = response.body();
                    List<WeatherData> weatherList = weatherResponse.getWeather();

                    if (weatherList != null && !weatherList.isEmpty()) {
                        selectedRegionWeather.addAll(weatherList);

                        // Сортируем по дате (сначала самые свежие)
                        weatherList.sort((w1, w2) -> w2.getDate().compareTo(w1.getDate()));

                        // Обновляем UI
                        updateWeatherUI(weatherList);

                        // Если тестовые данные, показываем сообщение
                        if (weatherResponse.isTestData()) {
                            Toast.makeText(AdminDashboardActivity.this,
                                    "Используются тестовые данные",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        showNoWeather("Нет данных по региону: " + selectedRegion);
                    }
                } else {
                    showNoWeather("Ошибка загрузки: " + response.code());
                    Log.e(TAG, "Ошибка загрузки погоды для региона: " + selectedRegion + ", код: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Log.e(TAG, "Ошибка сети для региона: " + selectedRegion, t);
                showNoWeather("Ошибка сети: " + t.getMessage());
            }
        });
    }

    private void loadAllPlants() {
        tvNoPlants.setVisibility(View.GONE);
        allPlants.clear();
        plantCategories.clear();

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

                    for (Crop plant : plants) {
                        plantCategories.put(String.valueOf(plant.getId()), category.getName());
                    }
                }
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

        tvPlantsCount.setText("Всего растений: " + allPlants.size());

        Map<String, Integer> categoryCount = new HashMap<>();
        for (Crop plant : allPlants) {
            String catName = plantCategories.get(String.valueOf(plant.getId()));
            if (catName != null) {
                categoryCount.put(catName, categoryCount.getOrDefault(catName, 0) + 1);
            }
        }

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

        tvId.setText(String.valueOf(plant.getId()));
        tvName.setText(plant.getName() != null ? plant.getName() : "-");

        String categoryName = plantCategories.get(String.valueOf(plant.getId()));
        tvCategory.setText(categoryName != null ? categoryName : "-");

        tvTempMin.setText(plant.getMinTemp() != null ? plant.getMinTemp() + "°C" : "-");
        tvTempMax.setText(plant.getMaxTemp() != null ? plant.getMaxTemp() + "°C" : "-");

        if (plant.getMinHumidity() != null && plant.getMaxHumidity() != null) {
            tvHumidity.setText(plant.getMinHumidity() + "-" + plant.getMaxHumidity() + "%");
        } else if (plant.getMinHumidity() != null) {
            tvHumidity.setText("от " + plant.getMinHumidity() + "%");
        } else if (plant.getMaxHumidity() != null) {
            tvHumidity.setText("до " + plant.getMaxHumidity() + "%");
        } else {
            tvHumidity.setText("-");
        }

        tvWind.setText(plant.getMaxWind() != null ? plant.getMaxWind() + " м/с" : "-");
        tvPrecipitation.setText(plant.getNeededPrecipitation() != null ? plant.getNeededPrecipitation() + " мм" : "-");
        tvSowingDepth.setText(plant.getSowingDepth() != null ? plant.getSowingDepth() + " см" : "-");

        btnDelete.setOnClickListener(v -> showDeletePlantDialog(plant));

        containerPlantsRows.addView(rowView);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(getResources().getColor(R.color.color_divider));
        containerPlantsRows.addView(divider);


        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this,
                    EditPlantActivityAdmin.class);
            intent.putExtra("CROP_ID", plant.getId());
            startActivityForResult(intent, 2);
        });
    }

    private void updateWeatherUI(List<WeatherData> weatherList) {
        if (weatherList.isEmpty()) {
            showNoWeather("Нет данных по региону: " + selectedRegion);
            return;
        }

        tvWeatherInfo.setText("Регион: " + selectedRegion + " | Записей: " + weatherList.size());

        // Отображаем все записи с правильными ID
        for (int i = 0; i < weatherList.size(); i++) {
            addWeatherRow(weatherList.get(i), i + 1); // i+1 для порядкового номера
        }
    }

    private void addWeatherRow(WeatherData weather, int rowNumber) {
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

        tvRegion.setText(selectedRegion);
//        tvDate.setText(weather.getDate() != null ? weather.getDate() : "-");
//        tvTemp.setText(weather.getTemperature() != null ? weather.getTemperature() : "-");
//        tvWind.setText(weather.getWind() != null ? weather.getWind() : "-");
//        tvPressure.setText(weather.getPressure() != null ? weather.getPressure() : "-");
//        tvHumidity.setText(weather.getHumidity() != null ? weather.getHumidity() : "-");
//        tvPrecipitation.setText(weather.getPrecipitation() != null ? weather.getPrecipitation() : "-");


        btnDelete.setOnClickListener(v -> {
            if (weather.getDate() != null) {
                deleteWeatherRecord(weather.getDate(), selectedRegion);
            } else {
                Toast.makeText(this, "Дата не указана", Toast.LENGTH_SHORT).show();
            }
        });

        containerWeatherRows.addView(rowView);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(getResources().getColor(R.color.color_divider));
        containerWeatherRows.addView(divider);
    }

    // Метод для удаления записи погоды
    private void deleteWeatherRecord(String date, String regionName) {
        Log.d(TAG, "Удаление записи: " + date + ", регион: " + regionName);

        // Показываем диалог подтверждения
        new MaterialAlertDialogBuilder(this)
                .setTitle("Удаление данных")
                .setMessage("Вы уверены, что хотите удалить данные за " + date + "?\n\n" +
                        "🗺️ Регион: " + regionName)
                .setPositiveButton("Удалить", (dialog, which) -> {
                    performDeleteWeather(date, regionName);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void performDeleteWeather(String date, String regionName) {
        // Показываем прогресс
        Toast.makeText(this, "Удаление...", Toast.LENGTH_SHORT).show();

        // Вызываем API удаления
        apiService.deleteWeatherByDateRegion(date, regionName).enqueue(new Callback<DeleteResponse>() {
            @Override
            public void onResponse(Call<DeleteResponse> call, Response<DeleteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DeleteResponse deleteResponse = response.body();

                    if (deleteResponse.isSuccess()) {
                        // Обновляем весь список
                        loadWeatherForSelectedRegion();

                        Toast.makeText(AdminDashboardActivity.this,
                                deleteResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AdminDashboardActivity.this,
                                "Ошибка: " + deleteResponse.getError(),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AdminDashboardActivity.this,
                            "Ошибка сервера: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DeleteResponse> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this,
                        "Ошибка сети: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Ошибка удаления записи", t);
            }
        });
    }

    private void showDeletePlantDialog(Crop plant) {
        String category = plantCategories.get(String.valueOf(plant.getId()));

        new MaterialAlertDialogBuilder(this)
                .setTitle("Удалить растение")
                .setMessage("Вы уверены, что хотите удалить растение?\n\n" +
                        "❌ Название: " + plant.getName() + "\n" +
                        "🏷️ ID: " + plant.getId() + "\n" +
                        "📁 Категория: " + (category != null ? category : "Неизвестно"))
                .setPositiveButton("Удалить", (dialog, which) -> {
                    Toast.makeText(this,
                            "Удаление растения \"" + plant.getName() + "\" (ID: " + plant.getId() + ") в разработке",
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
        tvWeatherInfo.setText("Регион: " + selectedRegion);
    }

    // В AdminDashboardActivity заменить методы:

    private void showAddPlantDialog() {
        // Вместо диалога открываем новую Activity
        Intent intent = new Intent(this, AddPlantActivityAdmin.class);
        startActivityForResult(intent, 1);
    }

    // Добавить метод для обработки результата
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            // Обновляем список растений после добавления/редактирования
            containerPlantsRows.removeAllViews();
            loadAllPlants();
        }
    }
}