package com.example.ars;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.Category;
import com.example.ars.models.Crop;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddPlantActivityAdmin extends AppCompatActivity {

    private TextInputEditText etPlantName, etDescription, etMinTemp, etMaxTemp,
            etMaxWind, etMinHumidity, etMaxHumidity, etNeededPrecipitation,
            etSowingDepth, etDaysToGermination, etDaysToHarvest,
            etPhotoPath;

    private AutoCompleteTextView actvCategory;
    private TextInputLayout tilCategory;
    private MaterialButton btnAddPlant;
    private ImageView btnBack;

    private ApiService apiService;
    private List<Category> categories = new ArrayList<>();
    private Integer selectedCategoryId = null;
    private String selectedCategoryName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_plant_admin);

        apiService = RetrofitClient.getApiService();
        initViews();
        setupListeners();
        loadCategories();
    }

    private void initViews() {
        // TextInputEditText поля
        etPlantName = findViewById(R.id.etPlantName);
        etDescription = findViewById(R.id.etDescription);
        etMinTemp = findViewById(R.id.etMinTemp);
        etMaxTemp = findViewById(R.id.etMaxTemp);
        etMaxWind = findViewById(R.id.etMaxWind);
        etMinHumidity = findViewById(R.id.etMinHumidity);
        etMaxHumidity = findViewById(R.id.etMaxHumidity);
        etNeededPrecipitation = findViewById(R.id.etNeededPrecipitation);
        etSowingDepth = findViewById(R.id.etSowingDepth);
        etDaysToGermination = findViewById(R.id.etDaysToGermination);
        etDaysToHarvest = findViewById(R.id.etDaysToHarvest);
        etPhotoPath = findViewById(R.id.etPhotoPath);

        // Выпадающий список категорий
        tilCategory = findViewById(R.id.tilCategory);
        actvCategory = findViewById(R.id.actvCategory);

        // Кнопки
        btnAddPlant = findViewById(R.id.btnAddPlant);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupListeners() {
        btnAddPlant.setOnClickListener(v -> addPlant());
        btnBack.setOnClickListener(v -> finish());

        // Обработчик выбора категории
        actvCategory.setOnItemClickListener((parent, view, position, id) -> {
            if (position < categories.size()) {
                Category selectedCategory = categories.get(position);
                selectedCategoryId = Math.toIntExact(selectedCategory.getId());
                selectedCategoryName = selectedCategory.getName();
                Log.d("AddPlant", "Selected category: " + selectedCategoryName + " (ID: " + selectedCategoryId + ")");
            }
        });
    }

    private void loadCategories() {
        apiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categories = response.body();
                    setupCategoryAdapter();
                } else {
                    Toast.makeText(AddPlantActivityAdmin.this,
                            "Ошибка загрузки категорий: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Toast.makeText(AddPlantActivityAdmin.this,
                        "Ошибка сети при загрузке категорий",
                        Toast.LENGTH_SHORT).show();
                Log.e("AddPlant", "Error loading categories", t);
            }
        });
    }

    private void setupCategoryAdapter() {
        List<String> categoryNames = new ArrayList<>();
        for (Category category : categories) {
            categoryNames.add(category.getName());
        }

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categoryNames
        );

        actvCategory.setAdapter(categoryAdapter);

        if (!categoryNames.isEmpty()) {
            // Выбрать первую категорию по умолчанию
            actvCategory.setText(categoryNames.get(0), false);
            selectedCategoryId = Math.toIntExact(categories.get(0).getId());
            selectedCategoryName = categories.get(0).getName();
        }
    }

    private void addPlant() {
        // Получаем данные из полей
        String name = etPlantName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        // Проверка обязательных полей
        if (name.isEmpty()) {
            tilCategory.setError("Название обязательно");
            etPlantName.requestFocus();
            return;
        }

        if (selectedCategoryId == null || selectedCategoryName.isEmpty()) {
            tilCategory.setError("Выберите категорию");
            actvCategory.requestFocus();
            return;
        }

        // Создаем объект растения
        Crop crop = new Crop();
        crop.setName(name);
        crop.setCategory(selectedCategoryName); // Отправляем название категории
        crop.setDescription(description.isEmpty() ? null : description);

        // Парсим числовые поля с обработкой пустых значений
        try {
            if (!etMinTemp.getText().toString().isEmpty()) {
                crop.setMinTemp(Float.parseFloat(etMinTemp.getText().toString()));
            }

            if (!etMaxTemp.getText().toString().isEmpty()) {
                crop.setMaxTemp(Float.parseFloat(etMaxTemp.getText().toString()));
            }

            if (!etMaxWind.getText().toString().isEmpty()) {
                crop.setMaxWind(Float.parseFloat(etMaxWind.getText().toString()));
            }

            if (!etMinHumidity.getText().toString().isEmpty()) {
                crop.setMinHumidity(Integer.parseInt(etMinHumidity.getText().toString()));
            }

            if (!etMaxHumidity.getText().toString().isEmpty()) {
                crop.setMaxHumidity(Integer.parseInt(etMaxHumidity.getText().toString()));
            }

            if (!etNeededPrecipitation.getText().toString().isEmpty()) {
                crop.setNeededPrecipitation(Float.parseFloat(etNeededPrecipitation.getText().toString()));
            }

            if (!etSowingDepth.getText().toString().isEmpty()) {
                crop.setSowingDepth(Integer.parseInt(etSowingDepth.getText().toString()));
            }

            if (!etDaysToGermination.getText().toString().isEmpty()) {
                crop.setDaysToGermination(Integer.parseInt(etDaysToGermination.getText().toString()));
            }

            if (!etDaysToHarvest.getText().toString().isEmpty()) {
                crop.setDaysToHarvest(Integer.parseInt(etDaysToHarvest.getText().toString()));
            }

            if (!etPhotoPath.getText().toString().isEmpty()) {
                crop.setPhotoPath(etPhotoPath.getText().toString());
            }

            // Устанавливаем значения по умолчанию
            crop.setCanSeedlings(true);
            crop.setCanDirectSow(true);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Ошибка в числовых полях: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            Log.e("AddPlant", "Number format error", e);
            return;
        }

        // Показываем прогресс
        btnAddPlant.setEnabled(false);
        btnAddPlant.setText("Добавление...");

        // Отправляем на сервер
        Call<Crop> call = apiService.addCrop(crop);
        call.enqueue(new Callback<Crop>() {
            @Override
            public void onResponse(Call<Crop> call, Response<Crop> response) {
                btnAddPlant.setEnabled(true);
                btnAddPlant.setText("Добавить растение");

                if (response.isSuccessful() && response.body() != null) {
                    Crop savedCrop = response.body();
                    Toast.makeText(AddPlantActivityAdmin.this,
                            "✅ Растение \"" + savedCrop.getName() + "\" добавлено!",
                            Toast.LENGTH_LONG).show();

                    // Возвращаемся назад с результатом
                    setResult(RESULT_OK);
                    finish();

                } else {
                    String errorMsg = "Ошибка добавления: ";
                    if (response.code() == 404) {
                        errorMsg += "Эндпоинт не найден. Проверьте URL";
                    } else if (response.code() == 500) {
                        errorMsg += "Ошибка сервера";
                    } else {
                        errorMsg += response.code() + " - " + response.message();
                    }

                    Toast.makeText(AddPlantActivityAdmin.this,
                            errorMsg,
                            Toast.LENGTH_LONG).show();

                    Log.e("AddPlant", "Error response: " + response.code() + " - " + response.message());
                    if (response.errorBody() != null) {
                        try {
                            Log.e("AddPlant", "Error body: " + response.errorBody().string());
                        } catch (Exception e) {
                            Log.e("AddPlant", "Error reading error body", e);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<Crop> call, Throwable t) {
                btnAddPlant.setEnabled(true);
                btnAddPlant.setText("Добавить растение");

                Toast.makeText(AddPlantActivityAdmin.this,
                        "❌ Ошибка сети: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();

                Log.e("AddPlant", "Network error", t);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}