package com.example.ars;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.cardview.widget.CardView;

public class AdminDashboardActivity extends BaseActivity {

    CardView btnPlants;
    CardView btnUsers;
    CardView btnRegions;
    CardView btnNavCategories;
    CardView btnNavWeather;

    @SuppressLint({"WrongViewCast", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        btnPlants = findViewById(R.id.btnNavPlants);
        btnUsers = findViewById(R.id.btnNavUsers);
        btnRegions = findViewById(R.id.btnNavRegions);
        btnNavCategories = findViewById(R.id.btnNavCategories);
        btnNavWeather = findViewById(R.id.btnNavWeather);

        // Кнопка выхода с полной очисткой данных
        findViewById(R.id.btnLogout).setOnClickListener(v -> logoutAndClear());

        btnPlants.setOnClickListener(v -> {
            Intent intent = new Intent(this, PlantsListActivityAdmin.class);
            startActivity(intent);
        });

        btnUsers.setOnClickListener(v -> {
            Intent intent = new Intent(this, UsersListActivityAdmin.class);
            startActivity(intent);
        });

        btnRegions.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegionsAdminActivity.class);
            startActivity(intent);
        });

        btnNavCategories.setOnClickListener(v -> {
            Intent intent = new Intent(this, CategoriesAdminActivity.class);
            startActivity(intent);
        });

        btnNavWeather.setOnClickListener(v -> {
            Intent intent = new Intent(this, WeatherAdminActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onBackPressed() {
        logoutAndClear();
    }

    private void logoutAndClear() {
        prefsHelper.clearAll();
        Toast.makeText(this, "Вы вышли из системы", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}