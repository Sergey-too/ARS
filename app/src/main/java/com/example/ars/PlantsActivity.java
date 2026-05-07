package com.example.ars;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.adapters.UserPlantAdapter;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.Crop;
import com.example.ars.models.UserCrop;
import com.example.ars.utils.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlantsActivity extends AppCompatActivity {

    private View sideMenuOverlay;
    private View sideMenu;
    private boolean isMenuOpen = false;
    private ApiService apiService;
    private SharedPreferencesHelper prefsHelper;
    private UserPlantAdapter adapter;
    private boolean isLoading = false;

    private List<UserPlantAdapter.PlantItem> combinedList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plants);

        prefsHelper = new SharedPreferencesHelper(this);
        RetrofitClient.initialize(prefsHelper);
        apiService = RetrofitClient.getApiService();

        setupSideMenu();
        setupRecyclerView();
        setupSimpleSearch();
        setupMenuButtons();

        findViewById(R.id.fabAdd).setOnClickListener(v ->
                startActivity(new Intent(this, AddPlantActivity.class))
        );

        loadAllData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllData();
    }

    private void setupSideMenu() {
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

        sideMenuOverlay.setOnClickListener(v -> closeSideMenu());
    }

    private void setupRecyclerView() {
        RecyclerView rvPlants = findViewById(R.id.rvPlants);
        rvPlants.setLayoutManager(new LinearLayoutManager(this));

        adapter = new UserPlantAdapter(combinedList, item -> {
            Intent intent = new Intent(this, PlantDetailActivity.class);
            if (item.isIndividual()) {
                intent.putExtra("individual_crop_id", item.getIndividualCropId());
                intent.putExtra("is_individual", true);
            } else {
                intent.putExtra("user_crop_id", item.getId());
                intent.putExtra("is_individual", false);
            }
            startActivity(intent);
        });
        rvPlants.setAdapter(adapter);
    }

    private void loadAllData() {
        if (prefsHelper.getUser() == null) return;
        if (isLoading) return;

        isLoading = true;
        Integer userId = prefsHelper.getUser().getId();

        combinedList.clear();
        adapter.updateData(combinedList);

        apiService.getUserCrops(userId).enqueue(new Callback<List<UserCrop>>() {
            @Override
            public void onResponse(@NonNull Call<List<UserCrop>> call, @NonNull Response<List<UserCrop>> response) {
                isLoading = false;

                if (response.isSuccessful() && response.body() != null) {
                    List<UserCrop> crops = response.body();
                    Log.d("PlantsActivity", "Загружено растений: " + crops.size());

                    for (UserCrop crop : crops) {
                        combinedList.add(new UserPlantAdapter.PlantItem(crop));
                    }

                    for (UserCrop crop : crops) {
                        if (crop.getCrop() == null && crop.getCropId() != null) {
                            loadSystemCropDetails(crop);
                        }
                    }

                    updateAdapter();
                } else {
                    Log.e("PlantsActivity", "Ошибка загрузки: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<UserCrop>> call, @NonNull Throwable t) {
                isLoading = false;
                Log.e("PlantsActivity", "Ошибка загрузки растений: " + t.getMessage());
                Toast.makeText(PlantsActivity.this, "Ошибка загрузки: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSystemCropDetails(UserCrop userCrop) {
        apiService.getCropById(userCrop.getCropId()).enqueue(new Callback<Crop>() {
            @Override
            public void onResponse(@NonNull Call<Crop> call, @NonNull Response<Crop> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userCrop.setCrop(response.body());
                    updateAdapter();
                }
            }
            @Override
            public void onFailure(@NonNull Call<Crop> call, @NonNull Throwable t) {
                Log.e("PlantsActivity", "Не удалось загрузить детали для CropID: " + userCrop.getCropId());
            }
        });
    }

    private void updateAdapter() {
        runOnUiThread(() -> {
            adapter.updateData(new ArrayList<>(combinedList));
        });
    }

    private void setupSimpleSearch() {
        com.google.android.material.textfield.TextInputEditText etSearch = findViewById(R.id.etSearch);
        if (etSearch == null) return;

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                filter(s.toString().trim().toLowerCase());
            }
        });
    }

    private void filter(String text) {
        List<UserPlantAdapter.PlantItem> filtered = new ArrayList<>();
        for (UserPlantAdapter.PlantItem item : combinedList) {
            if (item.getDisplayName().toLowerCase().contains(text)) {
                filtered.add(item);
            }
        }
        adapter.updateData(filtered);
    }

    private void setupMenuButtons() {
        findViewById(R.id.btnMenu).setOnClickListener(v -> openSideMenu());
        findViewById(R.id.btnCloseMenu).setOnClickListener(v -> closeSideMenu());

        findViewById(R.id.btnMenu1).setOnClickListener(v -> {
            closeSideMenu();
            startActivity(new Intent(this, PlantingRecommendationActivity.class));
        });
        findViewById(R.id.btnMenu2).setOnClickListener(v -> {
            closeSideMenu();
            startActivity(new Intent(this, WeatherActivity.class));
        });
        findViewById(R.id.btnMenu3).setOnClickListener(v -> showDeleteAllConfirmationDialog());
        findViewById(R.id.btnMenu4).setOnClickListener(v -> {
            closeSideMenu();
            startActivity(new Intent(this, AreasActivity.class));
        });
        findViewById(R.id.btnMenu5).setOnClickListener(v -> logout());
        findViewById(R.id.btnMenu6).setOnClickListener(v -> {
            closeSideMenu();
            startActivity(new Intent(this, WeatherStatsActivity.class));
        });
        findViewById(R.id.btnMenu7).setOnClickListener(v -> {
            closeSideMenu();
            startActivity(new Intent(this, CompatibilityActivity.class));
        });
        findViewById(R.id.btnMenu8).setOnClickListener(v -> {
            closeSideMenu();
            startActivity(new Intent(this, SupportListActivity.class));
        });
        findViewById(R.id.btnMenu9).setOnClickListener(v -> {
            closeSideMenu();
            startActivity(new Intent(this, UserCropsActivity.class));
        });
        findViewById(R.id.btnMenu10).setOnClickListener(v -> {
            closeSideMenu();
            startActivity(new Intent(this, TasksActivity.class));
        });
    }

    private void showDeleteAllConfirmationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Удаление")
                .setMessage("Очистить вашу коллекцию?")
                .setPositiveButton("Да", (d, w) -> deleteAllPlants())
                .setNegativeButton("Нет", null)
                .show();
    }

    private void deleteAllPlants() {
        apiService.deleteAllUserCrops(prefsHelper.getUser().getId()).enqueue(new Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<java.util.Map<String, Object>> call, @NonNull Response<java.util.Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    combinedList.clear();
                    adapter.updateData(combinedList);
                    closeSideMenu();
                    Toast.makeText(PlantsActivity.this, "Все растения удалены", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<java.util.Map<String, Object>> call, @NonNull Throwable t) {
                Toast.makeText(PlantsActivity.this, "Ошибка удаления: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
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
        sideMenuOverlay.animate().alpha(0f).setDuration(300)
                .withEndAction(() -> sideMenuOverlay.setVisibility(View.GONE));
        sideMenu.animate().translationX(sideMenu.getWidth()).setDuration(300);
    }
}