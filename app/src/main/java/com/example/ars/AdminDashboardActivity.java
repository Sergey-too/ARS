package com.example.ars;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class AdminDashboardActivity extends AppCompatActivity {

    CardView btnPlants;
    CardView btnUsers;


    @SuppressLint({"WrongViewCast", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        btnPlants = findViewById(R.id.btnNavPlants);
        btnUsers = findViewById(R.id.btnNavUsers);

        btnPlants.setOnClickListener(v -> {
            Intent intent = new Intent(this, PlantsListActivityAdmin.class);
            startActivity(intent);
        });

        btnUsers.setOnClickListener(v -> {
            Intent intent = new Intent(this, UsersListActivityAdmin.class);
            startActivity(intent);
        });
    }
}