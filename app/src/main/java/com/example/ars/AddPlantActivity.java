package com.example.ars;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.Area;
import com.example.ars.models.Category;
import com.example.ars.models.Crop;
import com.example.ars.models.IndividualUserCrop;
import com.example.ars.utils.SharedPreferencesHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddPlantActivity extends AppCompatActivity {

    private ApiService apiService;
    private SharedPreferencesHelper prefsHelper;

    private TextInputLayout tilCategory, tilCrop, tilArea;
    private AutoCompleteTextView actvPlantName;
    private TextView tvDescription;

    private List<Category> categories = new ArrayList<>();
    private List<Area> userAreas = new ArrayList<>();

    // Вспомогательный класс для объединения списков
    private class PlantListItem {
        Integer id;
        String name;
        String description;
        boolean isIndividual;

        PlantListItem(Integer id, String name, String description, boolean isIndividual) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.isIndividual = isIndividual;
        }

        @Override
        public String toString() {
            return isIndividual ? name + " (Моё)" : name;
        }
    }

    private List<PlantListItem> combinedPlantList = new ArrayList<>();

    private Integer selectedCropId = null;
    private Integer selectedIndividualCropId = null;
    private Integer selectedAreaId = null;
    private Integer selectedCategoryId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_plant);

        prefsHelper = new SharedPreferencesHelper(this);
        apiService = RetrofitClient.getApiService();

        initViews();
        loadCategories();
        loadUserAreas();
        setupDropdownListeners();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> showExitDialog());

        MaterialButton btnAddPlant = findViewById(R.id.btnAddPlant);
        btnAddPlant.setOnClickListener(v -> addPlantToUser());

        tilCategory = findViewById(R.id.tilPlantType);
        tilCrop = findViewById(R.id.tilPlantName);
        tilArea = findViewById(R.id.tilArea);
        actvPlantName = findViewById(R.id.actvPlantName);
        tvDescription = findViewById(R.id.tvPlantDescription);
    }

    private void loadUserAreas() {
        apiService.getUserAreas(prefsHelper.getUser().getId()).enqueue(new Callback<List<Area>>() {
            @Override
            public void onResponse(Call<List<Area>> call, Response<List<Area>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userAreas = response.body();
                    updateAreaDropdown();
                }
            }
            @Override public void onFailure(Call<List<Area>> call, Throwable t) {}
        });
    }

    private void updateAreaDropdown() {
        AutoCompleteTextView actvArea = findViewById(R.id.actvArea);
        ArrayAdapter<Area> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, userAreas);
        actvArea.setAdapter(adapter);
    }

    private void loadCategories() {
        apiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categories = response.body();
                    updateCategoryDropdown();
                }
            }
            @Override public void onFailure(Call<List<Category>> call, Throwable t) {}
        });
    }

    private void updateCategoryDropdown() {
        AutoCompleteTextView actvPlantType = findViewById(R.id.actvPlantType);
        List<String> categoryNames = new ArrayList<>();
        for (Category c : categories) {
            categoryNames.add(c.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categoryNames);
        actvPlantType.setAdapter(adapter);
    }

    private void setupDropdownListeners() {
        AutoCompleteTextView actvPlantType = findViewById(R.id.actvPlantType);
        AutoCompleteTextView actvArea = findViewById(R.id.actvArea);

        actvPlantType.setOnItemClickListener((parent, view, position, id) -> {
            // Берем объект из оригинального списка по позиции
            Category selected = categories.get(position);
            selectedCategoryId = selected.getId().intValue(); // Безопасное приведение
            tilCategory.setError(null);

            actvPlantName.setText("");
            selectedCropId = null;
            selectedIndividualCropId = null;
            tvDescription.setText("Выберите растение...");

            loadCombinedCrops(selected.getName(), selectedCategoryId);
        });

        // Слушатель для выбора растения (уже был правильным, но проверьте)
        actvPlantName.setOnItemClickListener((parent, view, position, id) -> {
            tilCrop.setError(null);
            PlantListItem selected = combinedPlantList.get(position);

            if (selected.isIndividual) {
                selectedIndividualCropId = selected.id;
                selectedCropId = null;
            } else {
                selectedCropId = selected.id;
                selectedIndividualCropId = null;
            }

            tvDescription.setText(selected.description != null ?
                    selected.description : "Описание отсутствует");
        });

        actvArea.setOnItemClickListener((parent, view, position, id) -> {
            tilArea.setError(null);
            selectedAreaId = userAreas.get(position).getId();
        });
    }

    private void loadCombinedCrops(String categoryName, Integer categoryId) {
        combinedPlantList.clear();

        // 1. Загружаем системные растения
        apiService.getCropsByCategory(categoryName).enqueue(new Callback<List<Crop>>() {
            @Override
            public void onResponse(Call<List<Crop>> call, Response<List<Crop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Crop c : response.body()) {
                        combinedPlantList.add(new PlantListItem(c.getId(), c.getName(), c.getDescription(), false));
                    }
                }
                // 2. Загружаем личные растения (синхронно после системных для порядка)
                loadIndividualCrops(categoryId);
            }
            @Override public void onFailure(Call<List<Crop>> call, Throwable t) { loadIndividualCrops(categoryId); }
        });
    }

    private void loadIndividualCrops(Integer categoryId) {
        apiService.getIndividualUserCrops(prefsHelper.getUser().getId()).enqueue(new Callback<List<IndividualUserCrop>>() {
            @Override
            public void onResponse(Call<List<IndividualUserCrop>> call, Response<List<IndividualUserCrop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (IndividualUserCrop ic : response.body()) {
                        // Фильтруем по категории, если она совпадает
                        if (ic.getCategoryId().equals(categoryId)) {
                            combinedPlantList.add(new PlantListItem(ic.getId(), ic.getName(), ic.getDescription(), true));
                        }
                    }
                }
                updatePlantDropdown();
            }
            @Override public void onFailure(Call<List<IndividualUserCrop>> call, Throwable t) { updatePlantDropdown(); }
        });
    }

    private void updatePlantDropdown() {
        ArrayAdapter<PlantListItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, combinedPlantList);
        actvPlantName.setAdapter(adapter);
    }

    private void addPlantToUser() {
        // Валидация перед отправкой
        if (selectedAreaId == null) {
            tilArea.setError("Выберите участок");
            return;
        }
        if (selectedCropId == null && selectedIndividualCropId == null) {
            tilCrop.setError("Выберите растение");
            return;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("userId", prefsHelper.getUser().getId());
        request.put("areaId", selectedAreaId);

        // ВАЖНО: Проверяем, что именно отправляем, чтобы не провоцировать 500 ошибку
        if (selectedCropId != null) {
            request.put("cropId", selectedCropId);
            // Если сервер падает от наличия ключа со значением null, не добавляйте его вообще
            // request.put("individualCropId", null);
        } else {
            request.put("individualCropId", selectedIndividualCropId);
            // request.put("cropId", null);
        }

        apiService.addUserCrop(request).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddPlantActivity.this, "Растение добавлено!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    // Если ошибка 500, попробуем прочитать лог ошибки
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Log.e("API_ERROR", "Code: " + response.code() + " Body: " + errorBody);
                    } catch (Exception e) { e.printStackTrace(); }

                    Toast.makeText(AddPlantActivity.this, "Ошибка сервера (500). Проверьте логи бэкенда.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(AddPlantActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Вернуться назад?")
                .setMessage("Все несохраненные данные будут потеряны.")
                .setPositiveButton("Да", (dialog, which) -> finish())
                .setNegativeButton("Нет", null)
                .show();
    }
}