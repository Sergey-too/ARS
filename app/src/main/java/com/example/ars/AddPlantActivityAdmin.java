package com.example.ars;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.Crop;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddPlantActivityAdmin extends AppCompatActivity {

    private EditText etName, etCategory, etDescription, etMinTemp, etMaxTemp,
            etMaxWind, etMinHumidity, etMaxHumidity, etNeededPrecipitation,
            etSowingDepth, etDaysToGermination, etDaysToHarvest,
            etPhotoPath;
    private Button btnAddPlant, btnBack;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_plant_admin);

        apiService = RetrofitClient.getApiService();
        initViews();
        setupListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.etPlantName);
        etCategory = findViewById(R.id.etCategory);
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

        btnAddPlant = findViewById(R.id.btnAddPlant);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupListeners() {
        btnAddPlant.setOnClickListener(v -> addPlant());
        btnBack.setOnClickListener(v -> finish());
    }

    private void addPlant() {
        // Получаем данные из полей
        String name = etName.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        // Проверка обязательных полей
        if (name.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Название и категория обязательны", Toast.LENGTH_SHORT).show();
            return;
        }

        // Создаем объект растения
        Crop crop = new Crop();
        crop.setName(name);
        crop.setCategory(category);
        crop.setDescription(description);

        // Парсим числовые поля
        try {
            if (!etMinTemp.getText().toString().isEmpty())
                crop.setMinTemp(Float.parseFloat(etMinTemp.getText().toString()));

            if (!etMaxTemp.getText().toString().isEmpty())
                crop.setMaxTemp(Float.parseFloat(etMaxTemp.getText().toString()));

            if (!etMaxWind.getText().toString().isEmpty())
                crop.setMaxWind(Float.parseFloat(etMaxWind.getText().toString()));

            if (!etMinHumidity.getText().toString().isEmpty())
                crop.setMinHumidity(Integer.parseInt(etMinHumidity.getText().toString()));

            if (!etMaxHumidity.getText().toString().isEmpty())
                crop.setMaxHumidity(Integer.parseInt(etMaxHumidity.getText().toString()));

            if (!etNeededPrecipitation.getText().toString().isEmpty())
                crop.setNeededPrecipitation(Float.parseFloat(etNeededPrecipitation.getText().toString()));

            if (!etSowingDepth.getText().toString().isEmpty())
                crop.setSowingDepth(Integer.parseInt(etSowingDepth.getText().toString()));

            if (!etDaysToGermination.getText().toString().isEmpty())
                crop.setDaysToGermination(Integer.parseInt(etDaysToGermination.getText().toString()));

            if (!etDaysToHarvest.getText().toString().isEmpty())
                crop.setDaysToHarvest(Integer.parseInt(etDaysToHarvest.getText().toString()));

            if (!etPhotoPath.getText().toString().isEmpty())
                crop.setPhotoPath(etPhotoPath.getText().toString());

            // По умолчанию
            crop.setCanSeedlings(true);
            crop.setCanDirectSow(true);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Ошибка в числовых полях", Toast.LENGTH_SHORT).show();
            return;
        }

        // Отправляем на сервер
        Call<Crop> call = apiService.addCrop(crop);
        call.enqueue(new Callback<Crop>() {
            @Override
            public void onResponse(Call<Crop> call, Response<Crop> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(AddPlantActivityAdmin.this,
                            "Растение добавлено!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AddPlantActivityAdmin.this,
                            "Ошибка: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e("AddPlant", "Error: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Crop> call, Throwable t) {
                Toast.makeText(AddPlantActivityAdmin.this,
                        "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("AddPlant", "Network error", t);
            }
        });
    }
}