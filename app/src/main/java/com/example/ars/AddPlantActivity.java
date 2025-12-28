package com.example.ars;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

public class AddPlantActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_plant);

        // Кнопка назад
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> showExitDialog());

        // Заполняем выпадающие списки
        setupDropdowns();

        // Кнопка добавления фото
        View photoCard = findViewById(R.id.photoCard);
        photoCard.setOnClickListener(v -> {
            Toast.makeText(this, "Добавление фото (в разработке)", Toast.LENGTH_SHORT).show();
        });

        // Кнопка добавления растения
        MaterialButton btnAddPlant = findViewById(R.id.btnAddPlant);
        btnAddPlant.setOnClickListener(v -> {
            // Добавление растения
            Toast.makeText(this, "Растение добавлено!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, PlantsActivity.class));
            finish();
        });
    }

    private void setupDropdowns() {
        // Типы растений
        String[] plantTypes = {"Цветы", "Овощи", "Фрукты", "Ягоды", "Травы", "Деревья", "Кустарники"};
        AutoCompleteTextView actvPlantType = findViewById(R.id.actvPlantType);
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, plantTypes);
        actvPlantType.setAdapter(typeAdapter);

        // Названия растений (зависит от типа)
        String[] plantNames = {"Помидор", "Огурец", "Перец", "Морковь", "Лук", "Картофель"};
        AutoCompleteTextView actvPlantName = findViewById(R.id.actvPlantName);
        ArrayAdapter<String> nameAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, plantNames);
        actvPlantName.setAdapter(nameAdapter);

        // Регионы (области РБ)
        String[] regions = {"Минская область", "Гомельская область", "Брестская область",
                "Гродненская область", "Витебская область", "Могилевская область"};
        AutoCompleteTextView actvRegion = findViewById(R.id.actvRegion);
        ArrayAdapter<String> regionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, regions);
        actvRegion.setAdapter(regionAdapter);
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Вернуться назад?")
                .setMessage("Все несохраненные данные будут потеряны. Вы уверены?")
                .setPositiveButton("Да", (dialog, which) -> {
                    startActivity(new Intent(this, PlantsActivity.class));
                    finish();
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        showExitDialog();
    }
}