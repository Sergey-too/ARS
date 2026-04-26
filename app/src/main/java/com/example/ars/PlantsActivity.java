package com.example.ars;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
    private UserPlantAdapter adapter;

    private List<UserCrop> combinedList = new ArrayList<>();
    private List<UserCrop> originalPlants = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plants);

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

        RecyclerView rvPlants = findViewById(R.id.rvPlants);
        rvPlants.setLayoutManager(new LinearLayoutManager(this));

        setupSimpleSearch();

        adapter = new UserPlantAdapter(combinedList, selectedCrop -> {
            Intent intent = new Intent(this, PlantDetailActivity.class);
            intent.putExtra("user_crop_id", selectedCrop.getId());
            boolean isIndividual = (selectedCrop.getCropId() == null);
            intent.putExtra("is_individual", isIndividual);

            startActivityForResult(intent, 1);
        });
        rvPlants.setAdapter(adapter);

        setupMenuButtons();

        findViewById(R.id.fabAdd).setOnClickListener(v -> startActivity(new Intent(this, AddPlantActivity.class)));

        loadAllData();
    }

    private void loadAllData() {
        if (prefsHelper.getUser() == null) return;
        Integer userId = prefsHelper.getUser().getId();

        combinedList.clear();
        originalPlants.clear();

        apiService.getUserCrops(userId).enqueue(new Callback<List<UserCrop>>() {
            @Override
            public void onResponse(Call<List<UserCrop>> call, Response<List<UserCrop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateMasterList(response.body());
                }
            }
            @Override public void onFailure(Call<List<UserCrop>> call, Throwable t) {}
        });

        apiService.getIndividualCrops(userId).enqueue(new Callback<List<UserCrop>>() {
            @Override
            public void onResponse(Call<List<UserCrop>> call, Response<List<UserCrop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateMasterList(response.body());
                }
            }
            @Override public void onFailure(Call<List<UserCrop>> call, Throwable t) {}
        });
    }


    private synchronized void updateMasterList(List<UserCrop> newItems) {
        boolean wasChanged = false;
        for (UserCrop newItem : newItems) {
            // Уникальный ключ: ID + наличие cropId (чтобы не путать системные и личные с одинаковым ID)
            boolean isNewIndividual = (newItem.getCropId() == null);

            boolean exists = false;
            for (UserCrop existingItem : combinedList) {
                boolean existingIsIndividual = (existingItem.getCropId() == null);
                if (existingItem.getId().equals(newItem.getId()) && isNewIndividual == existingIsIndividual) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                combinedList.add(newItem);
                wasChanged = true;
                // Если это системное растение и нет деталей — грузим один раз
                if (newItem.getCrop() == null && newItem.getCropId() != null) {
                    loadCropDetails(newItem.getCropId(), newItem);
                }
            }
        }

        if (wasChanged) {
            originalPlants = new ArrayList<>(combinedList);
            // Обновляем список целиком один раз, а не в цикле
            runOnUiThread(() -> adapter.updateData(new ArrayList<>(combinedList)));
        }
    }

    private void loadCropDetails(Integer cropId, UserCrop userCrop) {
        apiService.getCropById(cropId).enqueue(new Callback<Crop>() {
            @Override
            public void onResponse(Call<Crop> call, Response<Crop> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userCrop.setCrop(response.body());
                    // Используем notifyItemChanged вместо notifyDataSetChanged для плавности
                    runOnUiThread(() -> {
                        int pos = combinedList.indexOf(userCrop);
                        if (pos != -1) {
                            adapter.notifyItemChanged(pos);
                        }
                    });
                }
            }
            @Override public void onFailure(Call<Crop> call, Throwable t) {
                Log.e("PLANTS_ERR", "Не удалось загрузить детали для CropID: " + cropId);
            }
        });
    }

    // Чтобы удаление работало корректно и список обновлялся:
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Если в DetailActivity произошло удаление, полностью перегружаем список
            loadAllData();
        }
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
        List<UserCrop> filtered = new ArrayList<>();
        for (UserCrop uc : originalPlants) {
            String name = "";
            if (uc.getCrop() != null) name = uc.getCrop().getName();
            else if (uc.getName() != null) name = uc.getName();

            if (name.toLowerCase().contains(text)) {
                filtered.add(uc);
            }
        }
        adapter.updateData(filtered);
    }

    private void setupMenuButtons() {
        findViewById(R.id.btnMenu).setOnClickListener(v -> openSideMenu());
        findViewById(R.id.btnCloseMenu).setOnClickListener(v -> closeSideMenu());
        sideMenuOverlay.setOnClickListener(v -> closeSideMenu());

        findViewById(R.id.btnMenu1).setOnClickListener(v -> { closeSideMenu(); startActivity(new Intent(this, PlantingRecommendationActivity.class)); });
        findViewById(R.id.btnMenu2).setOnClickListener(v -> { closeSideMenu(); startActivity(new Intent(this, WeatherActivity.class)); });
        findViewById(R.id.btnMenu3).setOnClickListener(v -> showDeleteAllConfirmationDialog());
        findViewById(R.id.btnMenu4).setOnClickListener(v -> { closeSideMenu(); startActivity(new Intent(this, AreasActivity.class)); });
        findViewById(R.id.btnMenu5).setOnClickListener(v -> logout());
        findViewById(R.id.btnMenu6).setOnClickListener(v -> { closeSideMenu(); startActivity(new Intent(this, WeatherStatsActivity.class)); });
        findViewById(R.id.btnMenu7).setOnClickListener(v -> { closeSideMenu(); startActivity(new Intent(this, CompatibilityActivity.class)); });
        findViewById(R.id.btnMenu8).setOnClickListener(v -> { closeSideMenu(); startActivity(new Intent(this, SupportListActivity.class)); });
        findViewById(R.id.btnMenu9).setOnClickListener(v -> { closeSideMenu(); startActivity(new Intent(this, UserCropsActivity.class)); });
    }

    private void showDeleteAllConfirmationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Удаление")
                .setMessage("Очистить вашу коллекцию?")
                .setPositiveButton("Да", (d, w) -> deleteAllPlants())
                .setNegativeButton("Нет", null).show();
    }

    private void deleteAllPlants() {
        apiService.deleteAllUserCrops(prefsHelper.getUser().getId()).enqueue(new Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(Call<java.util.Map<String, Object>> call, Response<java.util.Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    synchronized (this) {
                        combinedList.clear();
                        originalPlants.clear();
                    }
                    adapter.updateData(combinedList);
                    closeSideMenu();
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
        loadAllData();
    }
}