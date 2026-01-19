package com.example.ars;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.Crop;
import com.example.ars.utils.SharedPreferencesHelper;
import com.squareup.picasso.Picasso;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlantDetailActivity extends AppCompatActivity {

    private ApiService apiService;
    private Integer cropId;
    private Crop currentCrop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_detail);

        // 1. Инициализация Retrofit
        apiService = RetrofitClient.getApiService();

        // 2. Получаем ID растения из Intent
        cropId = getIntent().getIntExtra("crop_id", -1);
        if (cropId == -1) {
            Toast.makeText(this, "Ошибка: не указано растение", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d("PlantDetail", "Открываем детали растения ID: " + cropId);

        // 3. Настройка кнопки назад
        setupBackButton();

        // 4. Настройка кнопки удаления (ДОБАВЬТЕ ЭТУ СТРОКУ)
        setupDeleteButton();

        // 5. Загружаем данные о растении
        loadPlantDetails();
    }

    // ==================== МЕТОДЫ НАСТРОЙКИ UI ====================

    private void setupBackButton() {
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack == null) {
            btnBack = new ImageView(this);
            btnBack.setImageResource(R.drawable.ic_arrow_back);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    // ==================== ЗАГРУЗКА ДАННЫХ ====================

    private void loadPlantDetails() {
        Log.d("PlantDetail", "Загружаю детали растения ID: " + cropId);

        // Показываем заглушки пока грузятся данные
        showLoadingState();

        // Вызываем API для получения данных о растении
        apiService.getCropById(cropId).enqueue(new Callback<Crop>() {
            @Override
            public void onResponse(Call<Crop> call, Response<Crop> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentCrop = response.body();
                    Log.d("PlantDetail", "Получено растение: " + currentCrop.getName());

                    // Обновляем UI с полученными данными
                    updateUI();
                } else {
                    Log.e("PlantDetail", "Ошибка загрузки: " + response.code());
                    Toast.makeText(PlantDetailActivity.this,
                            "Ошибка загрузки данных: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                    showDefaultData();
                }
            }

            @Override
            public void onFailure(Call<Crop> call, Throwable t) {
                Log.e("PlantDetail", "Ошибка сети: " + t.getMessage());
                Toast.makeText(PlantDetailActivity.this,
                        "Ошибка сети: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
                showDefaultData();
            }
        });
    }

    // ==================== ОБНОВЛЕНИЕ UI ====================

    private void showLoadingState() {
        // Можно показать ProgressBar или изменить текст на "Загрузка..."
        TextView tvPlantName = findViewById(R.id.tvPlantName);
        if (tvPlantName != null) {
            tvPlantName.setText("Загрузка...");
        }
    }

    private void updateUI() {
        if (currentCrop == null) return;

        // 1. Обновляем название растения
        updatePlantName();

        // 2. Обновляем фото растения
        updatePlantPhoto();

        // 3. Обновляем описание растения
        updatePlantDescription();

        // 4. Обновляем характеристики растения
        updatePlantCharacteristics();
    }

    private void updatePlantName() {
        TextView tvPlantName = findViewById(R.id.tvPlantName);
        if (tvPlantName != null) {
            tvPlantName.setText(currentCrop.getName());

            // Также можно установить заголовок в ActionBar
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(currentCrop.getName());
            }
        }
    }
    private void updatePlantPhoto() {
        ImageView ivPlantPhoto = findViewById(R.id.ivPlantPhoto);

        if (currentCrop.getPhotoPath() != null && !currentCrop.getPhotoPath().isEmpty()) {
            String photoPath = currentCrop.getPhotoPath();

            // Убираем начальный слэш если есть
            if (photoPath.startsWith("/")) {
                photoPath = photoPath.substring(1);
            }

            // Пробуем разные URL
            String[] urls = {
                    RetrofitClient.BASE_URL + "/api/img/" + photoPath,          // Основной
                    RetrofitClient.BASE_URL + "/api/images/" + photoPath,       // Альтернативный
                    RetrofitClient.BASE_URL + "/uploads/" + photoPath           // Прямой доступ
            };

            Log.d("PlantDetail", "Пробую загрузить фото: " + photoPath);

            // Используем цепочку попыток
            loadImageWithFallback(ivPlantPhoto, urls, 0);
        }
    }

    private void loadImageWithFallback(ImageView imageView, String[] urls, int index) {
        if (index >= urls.length) {
            Log.e("PlantDetail", "Все URL не сработали");
            return;
        }

        String url = urls[index];
        Log.d("PlantDetail", "Пробую URL [" + (index+1) + "]: " + url);

        Picasso.get()
                .load(url)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_close)
                .into(imageView, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        Log.d("PlantDetail", "✓ УСПЕХ! URL [" + (index+1) + "] сработал");
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("PlantDetail", "✗ URL [" + (index+1) + "] ошибка: " + e.getMessage());
                        // Пробуем следующий URL
                        loadImageWithFallback(imageView, urls, index + 1);
                    }
                });
    }

    private void updatePlantDescription() {
        TextView tvDescription = findViewById(R.id.tvDescription);
        if (tvDescription != null) {
            if (currentCrop.getDescription() != null && !currentCrop.getDescription().isEmpty()) {
                tvDescription.setText(currentCrop.getDescription());
                tvDescription.setVisibility(View.VISIBLE);
            } else {
                tvDescription.setText("Описание отсутствует");
            }
        }
    }

    private void updatePlantCharacteristics() {
        TextView tvRecommendations = findViewById(R.id.tvRecommendations);
        if (tvRecommendations != null) {
            StringBuilder characteristics = new StringBuilder();

            // 1. Категория
            if (currentCrop.getCategory() != null) {
                characteristics.append("📁 Категория: ")
                        .append(currentCrop.getCategory())
                        .append("\n\n");
            }

            // 2. Температурный режим
            if (currentCrop.getMinTemp() != null || currentCrop.getMaxTemp() != null) {
                characteristics.append("🌡️ Температура: ");
                if (currentCrop.getMinTemp() != null) {
                    characteristics.append("от ").append(currentCrop.getMinTemp()).append("°C");
                }
                if (currentCrop.getMaxTemp() != null) {
                    if (currentCrop.getMinTemp() != null) characteristics.append(" ");
                    characteristics.append("до ").append(currentCrop.getMaxTemp()).append("°C");
                }
                characteristics.append("\n\n");
            }

            // 3. Влажность
            if (currentCrop.getMinHumidity() != null || currentCrop.getMaxHumidity() != null) {
                characteristics.append("💧 Влажность: ");
                if (currentCrop.getMinHumidity() != null) {
                    characteristics.append("от ").append(currentCrop.getMinHumidity()).append("%");
                }
                if (currentCrop.getMaxHumidity() != null) {
                    if (currentCrop.getMinHumidity() != null) characteristics.append(" ");
                    characteristics.append("до ").append(currentCrop.getMaxHumidity()).append("%");
                }
                characteristics.append("\n\n");
            }

            // 4. Ветер
            if (currentCrop.getMaxWind() != null) {
                characteristics.append("💨 Максимальный ветер: ")
                        .append(currentCrop.getMaxWind())
                        .append(" м/с\n\n");
            }

            // 5. Осадки
            if (currentCrop.getNeededPrecipitation() != null) {
                characteristics.append("🌧️ Требуемые осадки: ")
                        .append(currentCrop.getNeededPrecipitation())
                        .append(" мм/месяц\n\n");
            }

            // 6. Глубина посадки
            if (currentCrop.getSowingDepth() != null) {
                characteristics.append("🕳️ Глубина посадки: ")
                        .append(currentCrop.getSowingDepth())
                        .append(" см\n\n");
            }

            // 7. Дни до всходов
            if (currentCrop.getDaysToGermination() != null) {
                characteristics.append("🌱 Дни до всходов: ")
                        .append(currentCrop.getDaysToGermination())
                        .append(" дней\n\n");
            }

            // 8. Дни до урожая
            if (currentCrop.getDaysToHarvest() != null) {
                characteristics.append("⏳ Дни до урожая: ")
                        .append(currentCrop.getDaysToHarvest())
                        .append(" дней\n\n");
            }

            // 9. Методы посадки
            boolean hasPlantingInfo = false;
            StringBuilder plantingMethods = new StringBuilder();

            if (Boolean.TRUE.equals(currentCrop.getCanSeedlings())) {
                plantingMethods.append("рассада");
                hasPlantingInfo = true;
            }

            if (Boolean.TRUE.equals(currentCrop.getCanDirectSow())) {
                if (hasPlantingInfo) plantingMethods.append(", ");
                plantingMethods.append("прямой посев");
                hasPlantingInfo = true;
            }

            if (hasPlantingInfo) {
                characteristics.append("🌿 Методы посадки: ")
                        .append(plantingMethods.toString())
                        .append("\n\n");
            }

            // Если есть характеристики, показываем их
            if (characteristics.length() > 0) {
                tvRecommendations.setText(characteristics.toString());

                // Показываем заголовок рекомендаций
                TextView tvRecommendationsTitle = findViewById(R.id.tvRecommendationsTitle);
                if (tvRecommendationsTitle != null) {
                    tvRecommendationsTitle.setText("Характеристики растения");
                }
            } else {
                tvRecommendations.setText("Характеристики растения отсутствуют в базе данных");
            }
        }
    }

    private void showDefaultData() {
        // Показываем данные по умолчанию если не удалось загрузить
        TextView tvPlantName = findViewById(R.id.tvPlantName);
        if (tvPlantName != null) {
            tvPlantName.setText("Растение #" + cropId);
        }

        TextView tvRecommendations = findViewById(R.id.tvRecommendations);
        if (tvRecommendations != null) {
            tvRecommendations.setText("Не удалось загрузить характеристики растения. Проверьте подключение к интернету.");
        }
    }

    private void setupDeleteButton() {
        com.google.android.material.button.MaterialButton btnDelete = findViewById(R.id.btnDelete);
        if (btnDelete == null) return;

        btnDelete.setOnClickListener(v -> {
            if (currentCrop == null || currentCrop.getId() == null) {
                Toast.makeText(this, "Не удалось получить данные о растении", Toast.LENGTH_SHORT).show();
                return;
            }

            // Получаем текущего пользователя
            com.example.ars.utils.SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(this);
            com.example.ars.models.User currentUser = prefsHelper.getUser();

            if (currentUser == null || currentUser.getId() == null) {
                Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
                return;
            }

            showDeleteConfirmationDialog(currentUser.getId(), currentCrop.getId());
        });
    }

    private void showDeleteConfirmationDialog(int userId, int cropId) {
        String plantName = currentCrop.getName() != null ? currentCrop.getName() : "это растение";

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Удаление растения")
                .setMessage("Вы действительно хотите удалить \"" + plantName + "\" из своей коллекции?")
                .setPositiveButton("Удалить", (dialog, which) -> deletePlantFromCollection(userId, cropId))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deletePlantFromCollection(int userId, int cropId) {
        // Показываем прогресс
        Toast.makeText(this, "Удаление...", Toast.LENGTH_SHORT).show();

        apiService.deleteUserCrop(userId, cropId).enqueue(new Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(Call<java.util.Map<String, Object>> call,
                                   Response<java.util.Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.Map<String, Object> result = response.body();
                    Boolean success = (Boolean) result.get("success");

                    if (success != null && success) {
                        Toast.makeText(PlantDetailActivity.this,
                                "Растение удалено из коллекции", Toast.LENGTH_SHORT).show();

                        // Возвращаемся на предыдущий экран
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        String error = (String) result.get("error");
                        Toast.makeText(PlantDetailActivity.this,
                                "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(PlantDetailActivity.this,
                            "Ошибка сервера: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<java.util.Map<String, Object>> call, Throwable t) {
                Toast.makeText(PlantDetailActivity.this,
                        "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==================== ДОПОЛНИТЕЛЬНЫЕ МЕТОДЫ ====================

    @Override
    public boolean onSupportNavigateUp() {
        // Обработка кнопки назад в ActionBar
        finish();
        return true;
    }
}