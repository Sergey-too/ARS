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
import com.example.ars.models.UserCrop;
import com.example.ars.utils.SharedPreferencesHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlantsActivity extends AppCompatActivity {

    private View sideMenuOverlay;
    private View sideMenu;
    private boolean isMenuOpen = false;
    private ApiService apiService;
    private SharedPreferencesHelper prefsHelper;

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

        RecyclerView rvPlants = findViewById(R.id.rvPlants);
        rvPlants.setLayoutManager(new LinearLayoutManager(this));

        // Загружаем тестовые данные (будут заменены на реальные)
        List<String> plants = new ArrayList<>();
        plants.add("Монстера - Добавлено: 12.03.2024");
        plants.add("Фикус - Добавлено: 10.03.2024");
        plants.add("Кактус - Добавлено: 05.03.2024");
        plants.add("Орхидея - Добавлено: 01.03.2024");
        plants.add("Суккулент - Добавлено: 28.02.2024");
        plants.add("Бамбук - Добавлено: 25.02.2024");

        SimpleAdapter adapter = new SimpleAdapter(plants);
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

    private void loadUserPlants() {
        com.example.ars.models.User currentUser = prefsHelper.getUser();
        if (currentUser == null || currentUser.getId() == null) {
            Log.e("PlantsActivity", "Пользователь не найден");
            return;
        }

        Log.d("PlantsActivity", "Загружаю растения для user ID: " + currentUser.getId());

        apiService.getUserCrops(currentUser.getId()).enqueue(new Callback<List<UserCrop>>() {
            @Override
            public void onResponse(Call<List<UserCrop>> call, Response<List<UserCrop>> response) {
                Log.d("PlantsActivity", "Ответ от сервера: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    List<UserCrop> userCrops = response.body();
                    Log.d("PlantsActivity", "Получено растений: " + userCrops.size());
                    updatePlantsList(userCrops);
                } else {
                    Log.e("PlantsActivity", "Ошибка ответа: " + response.code());
                    // Показываем тестовые данные если нет реальных
                    showTestData();
                }
            }

            @Override
            public void onFailure(Call<List<UserCrop>> call, Throwable t) {
                Log.e("PlantsActivity", "Ошибка сети", t);
                showTestData();
            }
        });
    }

    private void showTestData() {
        // Если нет данных с сервера, показываем тестовые
        List<String> testPlants = new ArrayList<>();
        testPlants.add("Монстера - Добавлено: 12.03.2024");
        testPlants.add("Фикус - Добавлено: 10.03.2024");
        testPlants.add("Кактус - Добавлено: 05.03.2024");

        SimpleAdapter adapter = new SimpleAdapter(testPlants);
        RecyclerView rvPlants = findViewById(R.id.rvPlants);
        rvPlants.setAdapter(adapter);
    }

    private void updatePlantsList(List<UserCrop> userCrops) {
        RecyclerView rvPlants = findViewById(R.id.rvPlants);

        if (userCrops.isEmpty()) {
            // Если нет растений, показываем сообщение
            List<String> emptyList = new ArrayList<>();
            emptyList.add("У вас пока нет растений");
            emptyList.add("Нажмите + чтобы добавить");

            SimpleAdapter adapter = new SimpleAdapter(emptyList);
            rvPlants.setAdapter(adapter);
            return;
        }

        List<String> plantsDisplay = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        for (UserCrop userCrop : userCrops) {
            String plantName = "Растение";
            if (userCrop.getCrop() != null && userCrop.getCrop().getName() != null) {
                plantName = userCrop.getCrop().getName();
            }

            String date = userCrop.getAddedDate() != null ?
                    sdf.format(userCrop.getAddedDate()) : "дата неизвестна";

            plantsDisplay.add(plantName + " - Добавлено: " + date);
        }

        SimpleAdapter adapter = new SimpleAdapter(plantsDisplay);
        rvPlants.setAdapter(adapter);
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

    class SimpleAdapter extends RecyclerView.Adapter<SimpleAdapter.ViewHolder> {
        private List<String> plants;

        SimpleAdapter(List<String> plants) {
            this.plants = plants;
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
            String plant = plants.get(position);
            holder.textView.setText(plant);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(PlantsActivity.this, PlantDetailActivity.class);
                intent.putExtra("plant_name", plant.split(" - ")[0]);
                intent.putExtra("notes", "Поливать раз в неделю");
                intent.putExtra("has_recommendations", true);
                startActivity(intent);
            });
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.TextView textView;

            ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.tvPlantName);
            }
        }
    }
}