package com.example.ars;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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

public class EditPlantActivityAdmin extends AppCompatActivity {

    private static final String TAG = "EditPlantActivity";

    private TextInputEditText etPlantName, etDescription, etMinTemp, etMaxTemp,
            etMaxWind, etMinHumidity, etMaxHumidity, etNeededPrecipitation,
            etSowingDepth, etDaysToGermination, etDaysToHarvest,
            etPhotoPath;

    private AutoCompleteTextView actvCategory;
    private TextInputLayout tilCategory;
    private MaterialButton btnSave, btnDelete;
    private ImageView btnBack;
    private TextView tvTitle, tvSubtitle;

    private ApiService apiService;
    private List<Category> categories = new ArrayList<>();
    private Integer selectedCategoryId = null;
    private String selectedCategoryName = "";

    private Crop currentCrop;
    private Integer cropId;
    private String originalCategoryName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_plant_admin);

        // Получаем ID растения из Intent
        cropId = getIntent().getIntExtra("CROP_ID", -1);
        if (cropId == -1) {
            Toast.makeText(this, "Ошибка: ID растения не указан", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiService = RetrofitClient.getApiService();
        initViews();
        setupListeners();
        loadCategories();
        loadCropData();
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
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        btnBack = findViewById(R.id.btnBack);

        // Текстовые поля
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> savePlant());
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        btnBack.setOnClickListener(v -> finish());

        // Обработчик выбора категории
        actvCategory.setOnItemClickListener((parent, view, position, id) -> {
            if (position < categories.size()) {
                Category selectedCategory = categories.get(position);
                selectedCategoryId = Math.toIntExact(selectedCategory.getId());
                selectedCategoryName = selectedCategory.getName();
                Log.d(TAG, "Selected category: " + selectedCategoryName + " (ID: " + selectedCategoryId + ")");
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
                    Toast.makeText(EditPlantActivityAdmin.this,
                            "Ошибка загрузки категорий",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Toast.makeText(EditPlantActivityAdmin.this,
                        "Ошибка сети при загрузке категорий",
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading categories", t);
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
    }

    private void loadCropData() {
        apiService.getCropById(cropId).enqueue(new Callback<Crop>() {
            @Override
            public void onResponse(Call<Crop> call, Response<Crop> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentCrop = response.body();
                    fillFormWithData();
                } else {
                    Toast.makeText(EditPlantActivityAdmin.this,
                            "Ошибка загрузки данных растения",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Crop> call, Throwable t) {
                Toast.makeText(EditPlantActivityAdmin.this,
                        "Ошибка сети при загрузке данных",
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading crop data", t);
                finish();
            }
        });
    }

    private void fillFormWithData() {
        if (currentCrop == null) return;

        // Заполняем поля данными
        tvTitle.setText("Редактирование растения");
        tvSubtitle.setText("ID: " + currentCrop.getId() + " • " + currentCrop.getName());

        etPlantName.setText(currentCrop.getName());

        // Категория
        originalCategoryName = currentCrop.getCategory();
        actvCategory.setText(originalCategoryName, false);
        selectedCategoryName = originalCategoryName;

        // Находим ID категории по имени
        for (Category category : categories) {
            if (category.getName().equals(originalCategoryName)) {
                selectedCategoryId = Math.toIntExact(category.getId());
                break;
            }
        }

        // Описание
        if (currentCrop.getDescription() != null) {
            etDescription.setText(currentCrop.getDescription());
        }

        // Температура
        if (currentCrop.getMinTemp() != null) {
            etMinTemp.setText(String.valueOf(currentCrop.getMinTemp()));
        }
        if (currentCrop.getMaxTemp() != null) {
            etMaxTemp.setText(String.valueOf(currentCrop.getMaxTemp()));
        }

        // Влажность
        if (currentCrop.getMinHumidity() != null) {
            etMinHumidity.setText(String.valueOf(currentCrop.getMinHumidity()));
        }
        if (currentCrop.getMaxHumidity() != null) {
            etMaxHumidity.setText(String.valueOf(currentCrop.getMaxHumidity()));
        }

        // Осадки
        if (currentCrop.getNeededPrecipitation() != null) {
            etNeededPrecipitation.setText(String.valueOf(currentCrop.getNeededPrecipitation()));
        }

        // Ветер
        if (currentCrop.getMaxWind() != null) {
            etMaxWind.setText(String.valueOf(currentCrop.getMaxWind()));
        }

        // Глубина посева
        if (currentCrop.getSowingDepth() != null) {
            etSowingDepth.setText(String.valueOf(currentCrop.getSowingDepth()));
        }

        // Дни
        if (currentCrop.getDaysToGermination() != null) {
            etDaysToGermination.setText(String.valueOf(currentCrop.getDaysToGermination()));
        }
        if (currentCrop.getDaysToHarvest() != null) {
            etDaysToHarvest.setText(String.valueOf(currentCrop.getDaysToHarvest()));
        }

        // Фото
        if (currentCrop.getPhotoPath() != null) {
            etPhotoPath.setText(currentCrop.getPhotoPath());
        }
    }

    private void savePlant() {
        // Получаем данные из полей
        String name = etPlantName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        // Проверка обязательных полей
        if (name.isEmpty()) {
            etPlantName.setError("Название обязательно");
            etPlantName.requestFocus();
            return;
        }

        if (selectedCategoryName.isEmpty()) {
            tilCategory.setError("Выберите категорию");
            actvCategory.requestFocus();
            return;
        }

        // Создаем объект растения с обновленными данными
        Crop updatedCrop = new Crop();
        updatedCrop.setName(name);
        updatedCrop.setCategory(selectedCategoryName);
        updatedCrop.setDescription(description.isEmpty() ? null : description);

        // Заполняем числовые поля
        try {
            if (!etMinTemp.getText().toString().isEmpty()) {
                updatedCrop.setMinTemp(Float.parseFloat(etMinTemp.getText().toString()));
            } else {
                updatedCrop.setMinTemp(null);
            }

            if (!etMaxTemp.getText().toString().isEmpty()) {
                updatedCrop.setMaxTemp(Float.parseFloat(etMaxTemp.getText().toString()));
            } else {
                updatedCrop.setMaxTemp(null);
            }

            if (!etMaxWind.getText().toString().isEmpty()) {
                updatedCrop.setMaxWind(Float.parseFloat(etMaxWind.getText().toString()));
            } else {
                updatedCrop.setMaxWind(null);
            }

            if (!etMinHumidity.getText().toString().isEmpty()) {
                updatedCrop.setMinHumidity(Integer.parseInt(etMinHumidity.getText().toString()));
            } else {
                updatedCrop.setMinHumidity(null);
            }

            if (!etMaxHumidity.getText().toString().isEmpty()) {
                updatedCrop.setMaxHumidity(Integer.parseInt(etMaxHumidity.getText().toString()));
            } else {
                updatedCrop.setMaxHumidity(null);
            }

            if (!etNeededPrecipitation.getText().toString().isEmpty()) {
                updatedCrop.setNeededPrecipitation(Float.parseFloat(etNeededPrecipitation.getText().toString()));
            } else {
                updatedCrop.setNeededPrecipitation(null);
            }

            if (!etSowingDepth.getText().toString().isEmpty()) {
                updatedCrop.setSowingDepth(Integer.parseInt(etSowingDepth.getText().toString()));
            } else {
                updatedCrop.setSowingDepth(null);
            }

            if (!etDaysToGermination.getText().toString().isEmpty()) {
                updatedCrop.setDaysToGermination(Integer.parseInt(etDaysToGermination.getText().toString()));
            } else {
                updatedCrop.setDaysToGermination(null);
            }

            if (!etDaysToHarvest.getText().toString().isEmpty()) {
                updatedCrop.setDaysToHarvest(Integer.parseInt(etDaysToHarvest.getText().toString()));
            } else {
                updatedCrop.setDaysToHarvest(null);
            }

            if (!etPhotoPath.getText().toString().isEmpty()) {
                updatedCrop.setPhotoPath(etPhotoPath.getText().toString());
            } else {
                updatedCrop.setPhotoPath(null);
            }

            // Булевые значения
            updatedCrop.setCanSeedlings(currentCrop != null ? currentCrop.getCanSeedlings() : true);
            updatedCrop.setCanDirectSow(currentCrop != null ? currentCrop.getCanDirectSow() : true);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Ошибка в числовых полях",
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Number format error", e);
            return;
        }

        // Показываем прогресс
        btnSave.setEnabled(false);
        btnSave.setText("Сохранение...");

        // Отправляем обновленные данные на сервер
        Call<Crop> call = apiService.updateCrop(cropId, updatedCrop);
        call.enqueue(new Callback<Crop>() {
            @Override
            public void onResponse(Call<Crop> call, Response<Crop> response) {
                btnSave.setEnabled(true);
                btnSave.setText("Сохранить");

                if (response.isSuccessful() && response.body() != null) {
                    Crop savedCrop = response.body();
                    Toast.makeText(EditPlantActivityAdmin.this,
                            "✅ Растение \"" + savedCrop.getName() + "\" обновлено!",
                            Toast.LENGTH_LONG).show();

                    // Возвращаемся назад с результатом
                    setResult(RESULT_OK);
                    finish();

                } else {
                    String errorMsg = "Ошибка обновления: " + response.code();
                    Toast.makeText(EditPlantActivityAdmin.this,
                            errorMsg,
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error updating crop: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Crop> call, Throwable t) {
                btnSave.setEnabled(true);
                btnSave.setText("Сохранить");

                Toast.makeText(EditPlantActivityAdmin.this,
                        "❌ Ошибка сети: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();

                Log.e(TAG, "Network error when updating", t);
            }
        });
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Удаление растения")
                .setMessage("Вы уверены, что хотите удалить растение \"" +
                        (currentCrop != null ? currentCrop.getName() : "") + "\"?\n\n" +
                        "Это действие нельзя отменить.")
                .setPositiveButton("Удалить", (dialog, which) -> deletePlant())
                .setNegativeButton("Отмена", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deletePlant() {
        // Показываем прогресс
        btnDelete.setEnabled(false);
        btnDelete.setText("Удаление...");

        // Отправляем запрос на удаление
        Call<Void> call = apiService.deleteCrop(cropId);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                btnDelete.setEnabled(true);
                btnDelete.setText("Удалить");

                if (response.isSuccessful()) {
                    Toast.makeText(EditPlantActivityAdmin.this,
                            "✅ Растение удалено!",
                            Toast.LENGTH_LONG).show();

                    // Возвращаемся с результатом удаления
                    setResult(RESULT_OK);
                    finish();

                } else {
                    String errorMsg = "Ошибка удаления: " + response.code();
                    Toast.makeText(EditPlantActivityAdmin.this,
                            errorMsg,
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error deleting crop: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnDelete.setEnabled(true);
                btnDelete.setText("Удалить");

                Toast.makeText(EditPlantActivityAdmin.this,
                        "❌ Ошибка сети при удалении",
                        Toast.LENGTH_LONG).show();

                Log.e(TAG, "Network error when deleting", t);
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}