package com.example.ars;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class AdminDashboardActivity extends AppCompatActivity {

    CardView btnPlants;


    @SuppressLint({"WrongViewCast", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        btnPlants = findViewById(R.id.btnNavPlants);
        btnPlants.setOnClickListener(v -> {
            Intent intent = new Intent(this, PlantsListActivityAdmin.class);
            startActivity(intent);
        });
    }
}