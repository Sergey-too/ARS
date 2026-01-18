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
    private List<UserCrop> originalPlants = new ArrayList<>();

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

        setupSimpleSearch();

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

        Button btnDeleteAll = findViewById(R.id.btnMenu3);
        btnDeleteAll.setOnClickListener(v -> showDeleteAllConfirmationDialog());

        Button btnWeather = findViewById(R.id.btnMenu2);
        btnWeather.setOnClickListener(v -> {
            startActivity(new Intent(PlantsActivity.this, WeatherActivity.class));
        });

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(PlantsActivity.this, AddPlantActivity.class));
        });
        
        loadUserPlants();
    }

    private void showDeleteAllConfirmationDialog() {
        if (userCrops.isEmpty()) {
            Toast.makeText(this, "У вас нет растений для удаления", Toast.LENGTH_SHORT).show();
            closeSideMenu();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Удаление всех растений")
                .setMessage("Вы действительно хотите удалить ВСЕ растения из своей коллекции? (" +
                        userCrops.size() + " растений)")
                .setPositiveButton("Удалить все", (dialog, which) -> deleteAllPlants())
                .setNegativeButton("Отмена", null)
                .show();
    }
    private void setupSimpleSearch() {
        com.google.android.material.textfield.TextInputEditText etSearch = findViewById(R.id.etSearch);
        if (etSearch == null) return;

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String searchText = s.toString().trim().toLowerCase();
                searchByName(searchText);
            }
        });

        com.google.android.material.textfield.TextInputLayout searchLayout = findViewById(R.id.searchLayout);
        if (searchLayout != null) {
            searchLayout.setEndIconOnClickListener(v -> {
                etSearch.setText("");
                adapter.updateData(originalPlants);
            });
        }
    }

    private void searchByName(String searchText) {
        if (originalPlants.isEmpty()) {
            originalPlants = new ArrayList<>(userCrops);
        }

        if (searchText.isEmpty()) {
            adapter.updateData(originalPlants);
            return;
        }

        List<UserCrop> searchResults = new ArrayList<>();

        for (UserCrop userCrop : originalPlants) {
            if (userCrop.getCrop() != null &&
                    userCrop.getCrop().getName() != null) {

                String plantName = userCrop.getCrop().getName().toLowerCase();
                if (plantName.contains(searchText)) {
                    searchResults.add(userCrop);
                }
            }
        }

        adapter.updateData(searchResults.isEmpty() ? originalPlants : searchResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserPlants();
    }

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

                    for (UserCrop userCrop : userCrops) {
                        if (userCrop.getCrop() == null) {
                            loadCropDetails(userCrop.getCropId(), userCrop);
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

    private void deleteAllPlants() {
        com.example.ars.models.User currentUser = prefsHelper.getUser();
        if (currentUser == null || currentUser.getId() == null) {
            Toast.makeText(this, "Ошибка: пользователь не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        int userId = currentUser.getId();

        // Показываем прогресс
        Toast.makeText(this, "Удаление всех растений...", Toast.LENGTH_SHORT).show();

        // Используем endpoint для массового удаления
        apiService.deleteAllUserCrops(userId).enqueue(new Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(Call<java.util.Map<String, Object>> call,
                                   Response<java.util.Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.Map<String, Object> result = response.body();
                    Boolean success = (Boolean) result.get("success");

                    if (success != null && success) {
                        runOnUiThread(() -> {
                            userCrops.clear();
                            adapter.updateData(userCrops);
                            Toast.makeText(PlantsActivity.this,
                                    "Все растения удалены", Toast.LENGTH_SHORT).show();
                            closeSideMenu();
                        });
                    } else {
                        String error = (String) result.get("error");
                        runOnUiThread(() -> {
                            Toast.makeText(PlantsActivity.this,
                                    "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                            closeSideMenu();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(PlantsActivity.this,
                                "Ошибка сервера", Toast.LENGTH_SHORT).show();
                        closeSideMenu();
                    });
                }
            }

            @Override
            public void onFailure(Call<java.util.Map<String, Object>> call, Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(PlantsActivity.this,
                            "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    closeSideMenu();
                });
            }
        });
    }
    private void loadCropDetails(Integer cropId, UserCrop userCrop) {
        apiService.getCropById(cropId).enqueue(new Callback<Crop>() {
            @Override
            public void onResponse(Call<Crop> call, Response<Crop> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Crop crop = response.body();
                    userCrop.setCrop(crop);
                    Log.d("PlantsActivity", "Загружено растение: " + crop.getName());

                    // Обновляем RecyclerView
                    runOnUiThread(() -> adapter.notifyDataSetChanged());
                }
            }

            @Override
            public void onFailure(Call<Crop> call, Throwable t) {
                Log.e("PlantsActivity", "Ошибка загрузки растения " + cropId, t);
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

            holder.textView.setText(plantName);

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