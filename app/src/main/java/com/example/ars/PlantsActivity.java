package com.example.ars;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class PlantsActivity extends AppCompatActivity {

    private View sideMenuOverlay;
    private View sideMenu;
    private boolean isMenuOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plants);

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
        btnLogout.setOnClickListener(v -> {
            startActivity(new Intent(PlantsActivity.this, LoginActivity.class));
        });

        Button btnWeather = findViewById(R.id.btnMenu2);
        btnWeather.setOnClickListener(v -> {
            startActivity(new Intent(PlantsActivity.this, WeatherActivity.class));
        });

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(PlantsActivity.this, AddPlantActivity.class));
        });
    }

    private void setupMenuButton(int buttonId, String action) {
        Button button = findViewById(buttonId);
        button.setOnClickListener(v -> {
            closeSideMenu();
        });
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