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

import com.example.ars.adapters.UserPlantAdapter;
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
    private UserPlantAdapter adapter; // Исправленный внешний адаптер
    private List<UserCrop> userCrops = new ArrayList<>();
    private List<UserCrop> originalPlants = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plants);

        prefsHelper = new SharedPreferencesHelper(this);
        apiService = RetrofitClient.getApiService();

        sideMenuOverlay = findViewById(R.id.sideMenuOverlay);
        sideMenu = findViewById(R.id.sideMenu);

        // Настройка ширины и позиции меню
        sideMenu.post(() -> {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int menuWidth = (int) (screenWidth * 0.7);
            android.view.ViewGroup.LayoutParams params = sideMenu.getLayoutParams();
            params.width = menuWidth;
            sideMenu.setLayoutParams(params);
            sideMenu.setTranslationX(menuWidth);
        });

        RecyclerView rvPlants = findViewById(R.id.rvPlants);
        rvPlants.setLayoutManager(new LinearLayoutManager(this));

        setupSimpleSearch();

        // Инициализация адаптера с обработкой клика
        adapter = new UserPlantAdapter(userCrops, selectedCrop -> {
            Intent intent = new Intent(this, PlantDetailActivity.class);
            intent.putExtra("crop_id", selectedCrop.getCropId());
            startActivity(intent);
        });
        rvPlants.setAdapter(adapter);

        // Кнопки управления меню
        ImageView btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> openSideMenu());

        ImageView btnCloseMenu = findViewById(R.id.btnCloseMenu);
        btnCloseMenu.setOnClickListener(v -> closeSideMenu());

        sideMenuOverlay.setOnClickListener(v -> closeSideMenu());

        // ТВОИ КНОПКИ МЕНЮ (Все 9 штук)
        Button btnPlantingRecommendations = findViewById(R.id.btnMenu1);
        btnPlantingRecommendations.setOnClickListener(v -> {
            closeSideMenu();
            startActivity(new Intent(this, PlantingRecommendationActivity.class));
        });

        Button btnWeather = findViewById(R.id.btnMenu2);
        btnWeather.setOnClickListener(v -> {
            closeSideMenu();
            startActivity(new Intent(this, WeatherActivity.class));
        });

        Button btnDeleteAll = findViewById(R.id.btnMenu3);
        btnDeleteAll.setOnClickListener(v -> showDeleteAllConfirmationDialog());

        Button btnAreasList = findViewById(R.id.btnMenu4);
        btnAreasList.setOnClickListener(v -> {
            closeSideMenu();
            startActivity(new Intent(this, AreasActivity.class));
        });

        Button btnLogout = findViewById(R.id.btnMenu5);
        btnLogout.setOnClickListener(v -> logout());

        Button btnWeatherStats = findViewById(R.id.btnMenu6);
        btnWeatherStats.setOnClickListener(v -> {
            closeSideMenu();
            startActivity(new Intent(this, WeatherStatsActivity.class));
        });

        Button btnCompatibillity = findViewById(R.id.btnMenu7);
        btnCompatibillity.setOnClickListener(v -> {
            closeSideMenu();
            startActivity(new Intent(this, CompatibilityActivity.class));
        });

        Button btnSupport = findViewById(R.id.btnMenu8);
        btnSupport.setOnClickListener(v -> {
            closeSideMenu();
            startActivity(new Intent(this, SupportListActivity.class));
        });

        Button btnUsersCrops = findViewById(R.id.btnMenu9);
        btnUsersCrops.setOnClickListener(v -> {
            closeSideMenu();
            startActivity(new Intent(this, UserCropsActivity.class));
        });

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(this, AddPlantActivity.class));
        });

        loadUserPlants();
    }

    private void setupSimpleSearch() {
        com.google.android.material.textfield.TextInputEditText etSearch = findViewById(R.id.etSearch);
        if (etSearch == null) return;

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                searchByName(s.toString().trim().toLowerCase());
            }
        });
    }

    private void searchByName(String searchText) {
        if (searchText.isEmpty()) {
            adapter.updateData(originalPlants);
            return;
        }
        List<UserCrop> results = new ArrayList<>();
        for (UserCrop uc : originalPlants) {
            if (uc.getCrop() != null && uc.getCrop().getName() != null) {
                if (uc.getCrop().getName().toLowerCase().contains(searchText)) {
                    results.add(uc);
                }
            }
        }
        adapter.updateData(results);
    }

    private void loadUserPlants() {
        com.example.ars.models.User currentUser = prefsHelper.getUser();
        if (currentUser == null || currentUser.getId() == null) return;

        apiService.getUserCrops(currentUser.getId()).enqueue(new Callback<List<UserCrop>>() {
            @Override
            public void onResponse(Call<List<UserCrop>> call, Response<List<UserCrop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userCrops = response.body();
                    originalPlants = new ArrayList<>(userCrops);

                    // Если Crop внутри null, подгружаем детали отдельно
                    for (UserCrop uc : userCrops) {
                        if (uc.getCrop() == null) {
                            loadCropDetails(uc.getCropId(), uc);
                        }
                    }
                    adapter.updateData(userCrops);
                }
            }
            @Override public void onFailure(Call<List<UserCrop>> call, Throwable t) {
                Log.e("PlantsActivity", "Ошибка сети", t);
            }
        });
    }

    private void loadCropDetails(Integer cropId, UserCrop userCrop) {
        apiService.getCropById(cropId).enqueue(new Callback<Crop>() {
            @Override
            public void onResponse(Call<Crop> call, Response<Crop> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userCrop.setCrop(response.body());
                    runOnUiThread(() -> adapter.notifyDataSetChanged());
                }
            }
            @Override public void onFailure(Call<Crop> call, Throwable t) {}
        });
    }

    private void showDeleteAllConfirmationDialog() {
        if (userCrops.isEmpty()) {
            Toast.makeText(this, "Нет растений для удаления", Toast.LENGTH_SHORT).show();
            return;
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Удаление")
                .setMessage("Удалить все растения?")
                .setPositiveButton("Да", (d, w) -> deleteAllPlants())
                .setNegativeButton("Нет", null)
                .show();
    }

    private void deleteAllPlants() {
        int userId = prefsHelper.getUser().getId();
        apiService.deleteAllUserCrops(userId).enqueue(new Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(Call<java.util.Map<String, Object>> call, Response<java.util.Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    userCrops.clear();
                    originalPlants.clear();
                    adapter.updateData(userCrops);
                    closeSideMenu();
                    Toast.makeText(PlantsActivity.this, "Удалено", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<java.util.Map<String, Object>> call, Throwable t) {}
        });
    }

    private void logout() {
        prefsHelper.clearAll();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openSideMenu() {
        isMenuOpen = true;
        findViewById(R.id.fabAdd).setVisibility(View.GONE);
        sideMenuOverlay.setVisibility(View.VISIBLE);
        sideMenuOverlay.animate().alpha(1f).setDuration(300);
        sideMenu.animate().translationX(0).setDuration(300);
    }

    private void closeSideMenu() {
        isMenuOpen = false;
        findViewById(R.id.fabAdd).setVisibility(View.VISIBLE);
        sideMenuOverlay.animate().alpha(0f).setDuration(300).withEndAction(() -> sideMenuOverlay.setVisibility(View.GONE));
        sideMenu.animate().translationX(sideMenu.getWidth()).setDuration(300);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserPlants();
    }
}