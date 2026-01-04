package com.example.ars;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.Crop;
import com.example.ars.models.UserCrop;
import com.example.ars.utils.SharedPreferencesHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlantsActivity extends AppCompatActivity {

    private View sideMenuOverlay;
    private View sideMenu;
    private boolean isMenuOpen = false;
    private ApiService apiService;
    private SharedPreferencesHelper prefsHelper;
    private PlantsAdapter adapter;
    private List<UserCrop> userCrops = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plants);

        // Инициализация
        prefsHelper = new SharedPreferencesHelper(this);
        apiService = RetrofitClient.getApiService();

        sideMenuOverlay = findViewById(R.id.sideMenuOverlay);
        sideMenu = findViewById(R.id.sideMenu);

        sideMenu.post(() -> {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int menuWidth = (int) (screenWidth * 0.7);

            android.view.ViewGroup.LayoutParams params = sideMenu.getLayoutParams();
            params.width = menuWidth;
            sideMenu.setLayoutParams(params);

            sideMenu.setTranslationX(menuWidth);
        });

        // Настройка RecyclerView
        RecyclerView rvPlants = findViewById(R.id.rvPlants);
        rvPlants.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PlantsAdapter(userCrops);
        rvPlants.setAdapter(adapter);

        ImageView btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> openSideMenu());

        ImageView btnCloseMenu = findViewById(R.id.btnCloseMenu);
        btnCloseMenu.setOnClickListener(v -> closeSideMenu());

        sideMenuOverlay.setOnClickListener(v -> closeSideMenu());

        setupMenuButton(R.id.btnMenu1, "Профиль");
        setupMenuButton(R.id.btnMenu2, "Уведомления");
        setupMenuButton(R.id.btnMenu3, "Настройки");

        Button btnLogout = findViewById(R.id.btnMenu4);
        btnLogout.setOnClickListener(v -> logout());

        Button btnWeather = findViewById(R.id.btnMenu2);
        btnWeather.setOnClickListener(v -> {
            startActivity(new Intent(PlantsActivity.this, WeatherActivity.class));
        });

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(PlantsActivity.this, AddPlantActivity.class));
        });

        // Загружаем реальные данные
        loadUserPlants();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Обновляем список при возвращении на экран
        loadUserPlants();
    }

    // В методе loadUserPlants()
    private void loadUserPlants() {
        com.example.ars.models.User currentUser = prefsHelper.getUser();
        if (currentUser == null || currentUser.getId() == null) {
            Log.e("PlantsActivity", "Пользователь не найден");
            showEmptyState();
            return;
        }

        Log.d("PlantsActivity", "Загружаю растения для user ID: " + currentUser.getId());

        apiService.getUserCrops(currentUser.getId()).enqueue(new Callback<List<UserCrop>>() {
            @Override
            public void onResponse(Call<List<UserCrop>> call, Response<List<UserCrop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userCrops = response.body();
                    Log.d("PlantsActivity", "Получено растений: " + userCrops.size());

                    // ПРОВЕРКА данных
                    for (UserCrop userCrop : userCrops) {
                        Log.d("PlantsActivity", "CropId: " + userCrop.getCropId());
                        Log.d("PlantsActivity", "Crop object: " + (userCrop.getCrop() != null ? "NOT NULL" : "NULL"));

                        if (userCrop.getCrop() != null) {
                            Log.d("PlantsActivity", "  Name: " + userCrop.getCrop().getName());
                            Log.d("PlantsActivity", "  Description: " + userCrop.getCrop().getDescription());
                        }
                    }

                    updatePlantsList(userCrops);
                } else {
                    Log.e("PlantsActivity", "Ошибка загрузки: " + response.code());
                    showEmptyState();
                }
            }

            @Override
            public void onFailure(Call<List<UserCrop>> call, Throwable t) {
                Log.e("PlantsActivity", "Ошибка сети", t);
                showEmptyState();
            }
        });
    }
    private void updatePlantsList(List<UserCrop> userCrops) {
        if (userCrops == null || userCrops.isEmpty()) {
            showEmptyState();
            return;
        }

        adapter.updateData(userCrops);
    }

    private void showEmptyState() {
        // Если нет растений, создаем пустой список
        userCrops.clear();
        adapter.updateData(userCrops);
    }

    // Метод выхода из аккаунта
    private void logout() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Выход")
                .setMessage("Вы действительно хотите выйти из аккаунта?")
                .setPositiveButton("Выйти", (dialog, which) -> performLogout())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void performLogout() {
        prefsHelper.clearAll();
        Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupMenuButton(int buttonId, String action) {
        Button button = findViewById(buttonId);
        if (button != null) {
            button.setOnClickListener(v -> closeSideMenu());
        }
    }

    private void openSideMenu() {
        if (isMenuOpen) return;

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setVisibility(View.GONE);

        sideMenuOverlay.setVisibility(View.VISIBLE);
        sideMenuOverlay.setAlpha(0f);
        sideMenuOverlay.animate().alpha(1f).setDuration(300).start();
        sideMenu.animate().translationX(0).setDuration(300).start();

        isMenuOpen = true;
    }

    private void closeSideMenu() {
        if (!isMenuOpen) return;

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setVisibility(View.VISIBLE);

        sideMenuOverlay.animate().alpha(0f).setDuration(300).withEndAction(() -> {
            sideMenuOverlay.setVisibility(View.GONE);
        }).start();

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int menuWidth = (int) (screenWidth * 0.7);
        sideMenu.animate().translationX(menuWidth).setDuration(300).start();

        isMenuOpen = false;
    }

    // Создай отдельный файл PlantsAdapter.java или оставь как внутренний класс:
    class PlantsAdapter extends RecyclerView.Adapter<PlantsAdapter.ViewHolder> {
        private List<UserCrop> plants;

        PlantsAdapter(List<UserCrop> plants) {
            this.plants = plants;
        }

        public void updateData(List<UserCrop> newPlants) {
            this.plants = newPlants;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_plant, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public int getItemCount() {
            return plants.size();
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            UserCrop userCrop = plants.get(position);
            int cropId = userCrop.getCropId();

            String plantName;

            // 1. Пробуем получить имя из объекта Crop
            if (userCrop.getCrop() != null) {
                Crop crop = userCrop.getCrop();
                if (crop.getName() != null && !crop.getName().isEmpty()) {
                    plantName = crop.getName();
                    Log.d("PlantsAdapter", "✅ Имя из crop объекта: " + plantName);
                } else {
                    plantName = "Неизвестное растение";
                    Log.d("PlantsAdapter", "⚠️ Crop есть, но имя пустое");
                }
            } else {
                plantName = "Неизвестное растение";
                Log.d("PlantsAdapter", "❌ Crop объект NULL для cropId=" + cropId);
            }

            // 3. УСТАНАВЛИВАЕМ ТЕКСТ!
            holder.textView.setText(plantName);

            // 4. Также установите описание если есть
            if (holder.descriptionView != null && userCrop.getCrop() != null) {
                String description = userCrop.getCrop().getDescription();
                if (description != null && !description.isEmpty()) {
                    holder.descriptionView.setText(description);
                    holder.descriptionView.setVisibility(View.VISIBLE);
                } else {
                    holder.descriptionView.setVisibility(View.GONE);
                }
            }

            holder.itemView.setOnClickListener(v -> {
                Log.d("PlantsAdapter", "Клик по растению: " + plantName + " (ID: " + cropId + ")");

                Intent intent = new Intent(PlantsActivity.this, PlantDetailActivity.class);
                intent.putExtra("crop_id", cropId);
                PlantsActivity.this.startActivity(intent);
            });
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.TextView textView;
            android.widget.TextView descriptionView;

            ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.tvPlantName);
                descriptionView = itemView.findViewById(R.id.tvPlantDescription); // если есть в layout
            }
        }
    }
}