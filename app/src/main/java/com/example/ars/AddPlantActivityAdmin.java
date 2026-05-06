package com.example.ars;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
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

    private TextInputEditText etPlantName, etVariety, etDescription, etMinTemp, etMaxTemp,
            etMinHumidity, etMaxHumidity, etNeededPrecipitation, etMaxWind,
            etSowingDepth, etDaysToGermination, etDaysToHarvest,
            etWateringInterval, etFertilizingInterval, etSoilCareInterval, etProtectionInterval;

    private TextInputLayout tilPlantName, tilCategory;

    private AutoCompleteTextView actvCategory;
    private CheckBox cbCanSeedlings, cbCanDirectSow;
    private MaterialButton btnSavePlant;
    private ImageView btnBack;

    private ApiService apiService;
    private List<Category> categoriesList = new ArrayList<>();
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
        // Основные поля
        etPlantName = findViewById(R.id.etPlantName);
        etVariety = findViewById(R.id.etVariety);
        etDescription = findViewById(R.id.etDescription);

        // Климат и параметры
        etMinTemp = findViewById(R.id.etMinTemp);
        etMaxTemp = findViewById(R.id.etMaxTemp);
        etMinHumidity = findViewById(R.id.etMinHumidity);
        etMaxHumidity = findViewById(R.id.etMaxHumidity);
        etNeededPrecipitation = findViewById(R.id.etNeededPrecipitation);
        etMaxWind = findViewById(R.id.etMaxWind);
        etSowingDepth = findViewById(R.id.etSowingDepth);
        etDaysToGermination = findViewById(R.id.etDaysToGermination);
        etDaysToHarvest = findViewById(R.id.etDaysToHarvest);

        // Интервалы ухода
        etWateringInterval = findViewById(R.id.etWateringInterval);
        etFertilizingInterval = findViewById(R.id.etFertilizingInterval);
        etSoilCareInterval = findViewById(R.id.etSoilCareInterval); // Если есть в XML
        etProtectionInterval = findViewById(R.id.etProtectionInterval); // Если есть в XML

        // Layouts для ошибок
        tilPlantName = findViewById(R.id.tilPlantName);
        tilCategory = findViewById(R.id.tilCategory);

        actvCategory = findViewById(R.id.actvCategory);
        cbCanSeedlings = findViewById(R.id.cbCanSeedlings);
        cbCanDirectSow = findViewById(R.id.cbCanDirectSow);
        btnSavePlant = findViewById(R.id.btnAddPlant);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSavePlant.setOnClickListener(v -> {
            if (validateFields()) {
                saveCropToDatabase();
            }
        });

        actvCategory.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategoryName = (String) parent.getItemAtPosition(position);
            tilCategory.setError(null);
        });
    }

    private void loadCategories() {
        apiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categoriesList = response.body();
                    List<String> names = new ArrayList<>();
                    for (Category c : categoriesList) names.add(c.getName());

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            AddPlantActivityAdmin.this,
                            android.R.layout.simple_dropdown_item_1line,
                            names
                    );
                    actvCategory.setAdapter(adapter);
                }
            }
            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Toast.makeText(AddPlantActivityAdmin.this, "Ошибка категорий", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateFields() {
        boolean isValid = true;

        tilPlantName.setError(null);
        tilCategory.setError(null);

        if (TextUtils.isEmpty(etPlantName.getText().toString().trim())) {
            tilPlantName.setError("Введите название");
            isValid = false;
        }

        if (TextUtils.isEmpty(selectedCategoryName)) {
            tilCategory.setError("Выберите категорию");
            isValid = false;
        }

        Float minT = parseSafeFloat(etMinTemp);
        Float maxT = parseSafeFloat(etMaxTemp);
        if (minT != null && maxT != null && minT > maxT) {
            Toast.makeText(this, "Мин. температура не может быть выше макс.", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        Integer minH = parseSafeInt(etMinHumidity);
        Integer maxH = parseSafeInt(etMaxHumidity);
        if ((minH != null && (minH < 0 || minH > 100)) || (maxH != null && (maxH < 0 || maxH > 100))) {
            Toast.makeText(this, "Влажность должна быть от 0 до 100%", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (minH != null && maxH != null && minH > maxH) {
            Toast.makeText(this, "Мин. влажность не может быть выше макс.", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (isNegative(etWateringInterval) || isNegative(etSowingDepth) || isNegative(etDaysToHarvest)) {
            Toast.makeText(this, "Значения не могут быть отрицательными", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private boolean isNegative(TextInputEditText et) {
        Integer val = parseSafeInt(et);
        return val != null && val < 0;
    }

    private void saveCropToDatabase() {
        Crop crop = new Crop();
        crop.setName(etPlantName.getText().toString().trim());
        crop.setVariety(etVariety.getText().toString().trim());
        crop.setCategory(selectedCategoryName);
        crop.setDescription(etDescription.getText().toString().trim());

        crop.setMinTemp(parseSafeFloat(etMinTemp));
        crop.setMaxTemp(parseSafeFloat(etMaxTemp));
        crop.setMinHumidity(parseSafeInt(etMinHumidity));
        crop.setMaxHumidity(parseSafeInt(etMaxHumidity));
        crop.setNeededPrecipitation(parseSafeFloat(etNeededPrecipitation));
        crop.setMaxWind(parseSafeFloat(etMaxWind));
        crop.setSowingDepth(parseSafeInt(etSowingDepth));
        crop.setDaysToGermination(parseSafeInt(etDaysToGermination));
        crop.setDaysToHarvest(parseSafeInt(etDaysToHarvest));

        crop.setWateringInterval(parseSafeInt(etWateringInterval));
        crop.setFertilizingInterval(parseSafeInt(etFertilizingInterval));
        if (etSoilCareInterval != null) crop.setSoilCareInterval(parseSafeInt(etSoilCareInterval));
        if (etProtectionInterval != null) crop.setProtectionInterval(parseSafeInt(etProtectionInterval));

        crop.setCanSeedlings(cbCanSeedlings.isChecked());
        crop.setCanDirectSow(cbCanDirectSow.isChecked());

        btnSavePlant.setEnabled(false);
        btnSavePlant.setText("СОХРАНЕНИЕ...");

        apiService.addCrop(crop).enqueue(new Callback<Crop>() {
            @Override
            public void onResponse(Call<Crop> call, Response<Crop> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddPlantActivityAdmin.this, "Добавлено!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    resetButton();
                    Toast.makeText(AddPlantActivityAdmin.this, "Ошибка сервера", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<Crop> call, Throwable t) {
                resetButton();
                Toast.makeText(AddPlantActivityAdmin.this, "Ошибка сети", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resetButton() {
        btnSavePlant.setEnabled(true);
        btnSavePlant.setText("СОХРАНИТЬ РАСТЕНИЕ");
    }

    private Float parseSafeFloat(TextInputEditText et) {
        if (et == null) return null;
        String text = et.getText().toString().trim();
        if (text.isEmpty()) return null;
        try { return Float.parseFloat(text.replace(",", ".")); }
        catch (NumberFormatException e) { return null; }
    }

    private Integer parseSafeInt(TextInputEditText et) {
        if (et == null) return null;
        String text = et.getText().toString().trim();
        if (text.isEmpty()) return null;
        try { return Integer.parseInt(text); }
        catch (NumberFormatException e) { return null; }
    }
}