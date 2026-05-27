package com.example.ars;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.adapters.AdminPlantAdapter;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.Category;
import com.example.ars.models.Crop;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlantsListActivityAdmin extends AppCompatActivity {

    private ApiService apiService;
    private AdminPlantAdapter adapter;
    private List<Crop> allPlants = new ArrayList<>();
    private AutoCompleteTextView actvCategoryFilter;
    private ProgressBar progressBar;
    private String currentCategory = "Все категории";
    private boolean isAscending = true;
    private TextInputEditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_plants);

        apiService = RetrofitClient.getApiService();
        progressBar = findViewById(R.id.progressBar);
        etSearch = findViewById(R.id.etSearch);
        actvCategoryFilter = findViewById(R.id.actvCategoryFilter);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        RecyclerView rvPlants = findViewById(R.id.rvPlants);
        rvPlants.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AdminPlantAdapter(new ArrayList<>(), plant -> {
            Intent intent = new Intent(this, EditPlantActivityAdmin.class);
            intent.putExtra("CROP_ID", plant.getId());
            startActivityForResult(intent, 100);
        });
        rvPlants.setAdapter(adapter);

        MaterialButton btnSortAlpha = findViewById(R.id.btnSortAlpha);
        btnSortAlpha.setOnClickListener(v -> {
            isAscending = !isAscending;
            applyFilters();
            Toast.makeText(this, isAscending ? "А-Я" : "Я-А", Toast.LENGTH_SHORT).show();
        });

        FloatingActionButton fabAdd = findViewById(R.id.fabAddPlant);
        fabAdd.setOnClickListener(v -> {
            startActivityForResult(new Intent(this, AddPlantActivityAdmin.class), 100);
        });

        setupSearch();
        loadCategories();
        loadPlants();
    }

    private void setupSearch() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    applyFilters();
                }
            });
        }
    }

    private void loadPlants() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        apiService.getAllCrops().enqueue(new Callback<List<Crop>>() {
            @Override
            public void onResponse(Call<List<Crop>> call, Response<List<Crop>> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    allPlants = response.body();
                    applyFilters();
                }
            }
            @Override
            public void onFailure(Call<List<Crop>> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(PlantsListActivityAdmin.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCategories() {
        apiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<String> catNames = new ArrayList<>();
                    catNames.add("Все категории");
                    for (Category c : response.body()) catNames.add(c.getName());

                    ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                            PlantsListActivityAdmin.this,
                            android.R.layout.simple_dropdown_item_1line,
                            catNames);
                    actvCategoryFilter.setAdapter(catAdapter);

                    actvCategoryFilter.setOnItemClickListener((parent, view, position, id) -> {
                        currentCategory = (String) parent.getItemAtPosition(position);
                        applyFilters();
                    });
                }
            }
            @Override public void onFailure(Call<List<Category>> call, Throwable t) {}
        });
    }

    private void applyFilters() {
        String query = (etSearch.getText() != null) ? etSearch.getText().toString().toLowerCase().trim() : "";
        List<Crop> filteredList = new ArrayList<>();

        for (Crop item : allPlants) {
            String name = (item.getName() != null) ? item.getName().toLowerCase() : "";
            String variety = (item.getVariety() != null) ? item.getVariety().toLowerCase() : "";
            String category = (item.getCategory() != null) ? item.getCategory() : "";

            boolean matchesSearch = name.contains(query) || variety.contains(query);

            boolean matchesCategory = currentCategory.equals("Все категории") || category.equals(currentCategory);

            if (matchesSearch && matchesCategory) {
                filteredList.add(item);
            }
        }

        Collections.sort(filteredList, (o1, o2) -> {
            String n1 = (o1.getName() != null) ? o1.getName() : "";
            String n2 = (o2.getName() != null) ? o2.getName() : "";
            int res = n1.compareToIgnoreCase(n2);
            return isAscending ? res : -res;
        });

        adapter.updateData(filteredList);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadPlants();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPlants();
    }
}