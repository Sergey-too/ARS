package com.example.ars;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.adapters.RegionAdapter;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.Region;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegionsAdminActivity extends AppCompatActivity {

    private RecyclerView rvRegions;
    private RegionAdapter adapter;
    private ApiService apiService;
    private List<Region> regionList = new ArrayList<>();
    private FloatingActionButton fabAddRegion;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_regions);

        initViews();
        setupRetrofit();
        setupRecyclerView();

        loadRegions();

        fabAddRegion.setOnClickListener(v -> showRegionDialog(null));
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void initViews() {
        rvRegions = findViewById(R.id.rvRegions);
        fabAddRegion = findViewById(R.id.fabAddRegion);
        btnBack = findViewById(R.id.btnBack);
        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("Управление регионами");
    }

    private void setupRetrofit() {
        apiService = RetrofitClient.getApiService();
    }

    private void setupRecyclerView() {
        // В адаптер передаем слушатель клика для редактирования
        adapter = new RegionAdapter(regionList, this::showRegionDialog);
        rvRegions.setLayoutManager(new LinearLayoutManager(this));
        rvRegions.setAdapter(adapter);
    }

    private void loadRegions() {
        apiService.getRegions().enqueue(new Callback<List<Region>>() {
            @Override
            public void onResponse(Call<List<Region>> call, Response<List<Region>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    regionList.clear();
                    regionList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(RegionsAdminActivity.this, "Ошибка загрузки", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Region>> call, Throwable t) {
                Toast.makeText(RegionsAdminActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRegionDialog(Region region) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(region == null ? "Новый регион" : "Редактировать");

        final EditText input = new EditText(this);
        input.setHint("Введите название");
        if (region != null) {
            input.setText(region.getName());
        }

        // Настройка отступов внутри диалога
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 60;
        params.rightMargin = 60;
        params.topMargin = 20;
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) return;

            if (region == null) {
                createRegion(new Region(name));
            } else {
                region.setName(name);
                updateRegion(region);
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void createRegion(Region region) {
        apiService.createRegion(region).enqueue(new Callback<Region>() {
            @Override
            public void onResponse(Call<Region> call, Response<Region> response) {
                if (response.isSuccessful()) {
                    loadRegions();
                    Toast.makeText(RegionsAdminActivity.this, "Регион добавлен", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Region> call, Throwable t) {
                Toast.makeText(RegionsAdminActivity.this, "Ошибка при создании", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRegion(Region region) {
        apiService.updateRegion(region.getId(), region).enqueue(new Callback<Region>() {
            @Override
            public void onResponse(Call<Region> call, Response<Region> response) {
                if (response.isSuccessful()) {
                    loadRegions();
                    Toast.makeText(RegionsAdminActivity.this, "Обновлено", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Region> call, Throwable t) {
                Toast.makeText(RegionsAdminActivity.this, "Ошибка при обновлении", Toast.LENGTH_SHORT).show();
            }
        });
    }
}