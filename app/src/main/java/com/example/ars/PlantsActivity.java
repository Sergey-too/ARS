package com.example.ars;

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
            closeSideMenu();
            finish();
        });

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            plants.add(0, "Новое растение - Добавлено: сегодня");
            adapter.notifyItemInserted(0);
            rvPlants.scrollToPosition(0);
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
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.textView.setText(plants.get(position));
        }

        @Override
        public int getItemCount() {
            return plants.size();
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