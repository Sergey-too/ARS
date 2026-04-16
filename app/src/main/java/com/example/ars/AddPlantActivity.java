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

    private List<Category> categories = new ArrayList<>();
    private List<Crop> cropsByCategory = new ArrayList<>();
    private List<Area> userAreas = new ArrayList<>();

    private Integer selectedCropId = null;
    private Integer selectedAreaId = null;
    private String selectedCategoryName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_plant);

        prefsHelper = new SharedPreferencesHelper(this);
        apiService = RetrofitClient.getApiService();

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> showExitDialog());

        loadCategories();
        loadUserAreas();

        setupDropdownListeners();

        MaterialButton btnAddPlant = findViewById(R.id.btnAddPlant);
        btnAddPlant.setOnClickListener(v -> addPlantToUser());

        tilCategory = findViewById(R.id.tilPlantType);
        tilCrop = findViewById(R.id.tilPlantName);
        tilArea = findViewById(R.id.tilArea);
    }

    private void loadUserAreas() {
        Integer userId = prefsHelper.getUser().getId();
        apiService.getUserAreas(userId).enqueue(new Callback<List<Area>>() {
            @Override
            public void onResponse(Call<List<Area>> call, Response<List<Area>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userAreas = response.body();
                    updateAreaDropdown();
                } else {
                    Toast.makeText(AddPlantActivity.this, "Участки не найдены", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Area>> call, Throwable t) {
                Toast.makeText(AddPlantActivity.this, "Ошибка загрузки участков", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateAreaDropdown() {
        AutoCompleteTextView actvArea = findViewById(R.id.actvArea);
        ArrayAdapter<Area> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, userAreas);
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

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Toast.makeText(AddPlantActivity.this, "Ошибка загрузки категорий", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void updateCategoryDropdown() {
        AutoCompleteTextView actvPlantType = findViewById(R.id.actvPlantType);
        List<String> names = new ArrayList<>();
        for (Category c : categories) names.add(c.getName());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, names);
        actvPlantType.setAdapter(adapter);
    }
    private void setupDropdownListeners() {
        AutoCompleteTextView actvPlantType = findViewById(R.id.actvPlantType);
        AutoCompleteTextView actvPlantName = findViewById(R.id.actvPlantName);
        AutoCompleteTextView actvArea = findViewById(R.id.actvArea);
        TextView tvDescription = findViewById(R.id.tvPlantDescription);

        actvPlantType.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategoryName = categories.get(position).getName();
            tilCategory.setError(null);

            actvPlantName.setText("");
            selectedCropId = null;

            loadCropsByCategoryName(selectedCategoryName);
        });

        actvPlantName.setOnTouchListener((v, event) -> {
            if (selectedCategoryName == null) {
                tilCategory.setError("Сначала выберите категорию");
                return true;
            }
            return false;
        });

        actvPlantName.setOnItemClickListener((parent, view, position, id) -> {
            tilCrop.setError(null);
            Crop selectedCrop = cropsByCategory.get(position);
            selectedCropId = selectedCrop.getId();
            tvDescription.setText(selectedCrop.getDescription() != null ?
                    selectedCrop.getDescription() : "Описание отсутствует");
        });

        actvArea.setOnItemClickListener((parent, view, position, id) -> {
            tilArea.setError(null);
            selectedAreaId = userAreas.get(position).getId();
        });
    }
    private void loadCropsByCategoryName(String categoryName) {
        apiService.getCropsByCategory(categoryName).enqueue(new Callback<List<Crop>>() {
            @Override
            public void onResponse(Call<List<Crop>> call, Response<List<Crop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cropsByCategory = response.body();
                    updateCropNamesDropdown();
                }
            }
            @Override public void onFailure(Call<List<Crop>> call, Throwable t) {}
        });
    }

    private void updateCropNamesDropdown() {
        AutoCompleteTextView actvPlantName = findViewById(R.id.actvPlantName);
        List<String> names = new ArrayList<>();
        for (Crop c : cropsByCategory) names.add(c.getName());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, names);
        actvPlantName.setAdapter(adapter);
    }

    private void addPlantToUser() {
        tilCategory.setError(null);
        tilCrop.setError(null);
        tilArea.setError(null);

        if (selectedCategoryName == null) {
            tilCategory.setError("Обязательное поле");
            return;
        }
        if (selectedCropId == null) {
            tilCrop.setError("Обязательное поле");
            return;
        }
        if (selectedAreaId == null) {
            tilArea.setError("Обязательное поле");
            return;
        }

        if (selectedCropId == null || selectedAreaId == null) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("userId", prefsHelper.getUser().getId());
        request.put("cropId", selectedCropId);
        request.put("areaId", selectedAreaId);

        apiService.addUserCrop(request).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddPlantActivity.this, "Добавлено!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AddPlantActivity.this, "Ошибка: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(AddPlantActivity.this, "Сеть недоступна", Toast.LENGTH_SHORT).show();
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