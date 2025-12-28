package com.example.ars;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class PlantDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_detail);

        // Получаем данные из Intent (заглушки пока)
        Intent intent = getIntent();
        String plantName = intent.getStringExtra("plant_name");
        String plantType = intent.getStringExtra("plant_type");
        String dateAdded = intent.getStringExtra("date_added");
        String region = intent.getStringExtra("region");
        String notes = intent.getStringExtra("notes");
        boolean hasRecommendations = intent.getBooleanExtra("has_recommendations", false);

        // Если данных нет, используем заглушки
        if (plantName == null) {
            plantName = "Монстера";
            plantType = "Декоративное комнатное";
            dateAdded = "12.03.2024";
            region = "Минская область";
            notes = "Поливать раз в неделю. Любит рассеянный свет. Пересаживать весной.";
            hasRecommendations = true;
        }

        // Заголовок с названием растения
        TextView tvPlantName = findViewById(R.id.tvPlantName);
        tvPlantName.setText(plantName);

        // Фото растения (заглушка пока)
        ImageView ivPlantPhoto = findViewById(R.id.ivPlantPhoto);
        // TODO: Загрузка фото из БД/URL
        // ivPlantPhoto.setImageURI(Uri.parse(photoUrl));

        // Заметки пользователя
        TextView tvNotes = findViewById(R.id.tvNotes);
        TextView tvNoNotes = findViewById(R.id.tvNoNotes);

        if (notes != null && !notes.isEmpty()) {
            tvNotes.setText(notes);
            tvNotes.setVisibility(View.VISIBLE);
            tvNoNotes.setVisibility(View.GONE);
        } else {
            tvNotes.setVisibility(View.GONE);
            tvNoNotes.setVisibility(View.VISIBLE);
        }

        // Кнопка редактирования заметок
        ImageView btnEditNotes = findViewById(R.id.btnEditNotes);
        btnEditNotes.setOnClickListener(v -> {
            // TODO: Открыть редактор заметок
            // startActivity(new Intent(this, EditNotesActivity.class));
        });

        // Рекомендации по посадке
        TextView tvRecommendations = findViewById(R.id.tvRecommendations);
        TextView tvNoRecommendations = findViewById(R.id.tvNoRecommendations);

        if (hasRecommendations) {
            // TODO: Загрузить рекомендации из БД
            String recommendations =
                    "1. Лучшее время для посадки: весна\n" +
                            "2. Почва: легкая, дренированная\n" +
                            "3. Освещение: рассеянный свет\n" +
                            "4. Полив: умеренный, не допускать переувлажнения\n" +
                            "5. Температура: 18-25°C";

            tvRecommendations.setText(recommendations);
            tvRecommendations.setVisibility(View.VISIBLE);
            tvNoRecommendations.setVisibility(View.GONE);
        } else {
            tvRecommendations.setVisibility(View.GONE);
            tvNoRecommendations.setVisibility(View.VISIBLE);
        }
    }

    // Метод для обновления данных (будет вызываться при возврате из редактора)
    public void updateNotes(String newNotes) {
        TextView tvNotes = findViewById(R.id.tvNotes);
        TextView tvNoNotes = findViewById(R.id.tvNoNotes);

        if (newNotes != null && !newNotes.isEmpty()) {
            tvNotes.setText(newNotes);
            tvNotes.setVisibility(View.VISIBLE);
            tvNoNotes.setVisibility(View.GONE);
        } else {
            tvNotes.setVisibility(View.GONE);
            tvNoNotes.setVisibility(View.VISIBLE);
        }
    }
}