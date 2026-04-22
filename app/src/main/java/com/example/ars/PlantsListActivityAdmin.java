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

import com.example.ars.adapters.PlantAdapter;
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
    private PlantAdapter adapter;
    private List<Crop> allPlants = new ArrayList<>();
    private AutoCompleteTextView actvCategoryFilter;
    private ProgressBar progressBar;
    private String currentCategory = "Все категории";
    private boolean isAscending = true;
    private List<Category> allCategories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_plants);

        apiService = RetrofitClient.getApiService();

        progressBar = findViewById(R.id.progressBar);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        RecyclerView rvPlants = findViewById(R.id.rvPlants);
        rvPlants.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PlantAdapter(new ArrayList<>(), plant -> {
            Intent intent = new Intent(this, EditPlantActivityAdmin.class);
            intent.putExtra("CROP_ID", plant.getId());
            startActivityForResult(intent, 100);
        });
        rvPlants.setAdapter(adapter);

        MaterialButton btnSortAlpha = findViewById(R.id.btnSortAlpha);
        btnSortAlpha.setOnClickListener(v -> {
            isAscending = !isAscending;
            applyFilters();

            Toast.makeText(this, isAscending ? "Сортировка: А-Я" : "Сортировка: Я-А", Toast.LENGTH_SHORT).show();
        });

        setupSearch();
        loadCategories();

        FloatingActionButton fabAdd = findViewById(R.id.fabAddPlant);
        fabAdd.setOnClickListener(v -> {
            startActivityForResult(new Intent(this, AddPlantActivityAdmin.class), 100);
        });

        loadPlants();
    }

    private void setupSearch() {
        TextInputEditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                applyFilters();
            }
        });
    }

    private void filter(String text) {
        List<Crop> filteredList = new ArrayList<>();
        for (Crop item : allPlants) {
            if (item.getName().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }
        adapter.updateData(filteredList);
    }

    private void loadPlants() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getAllCrops().enqueue(new Callback<List<Crop>>() {
            @Override
            public void onResponse(Call<List<Crop>> call, Response<List<Crop>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    allPlants = response.body();
                    adapter.updateData(allPlants);
                }
            }

            @Override
            public void onFailure(Call<List<Crop>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(PlantsListActivityAdmin.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCategories() {
        actvCategoryFilter = findViewById(R.id.actvCategoryFilter);
        apiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allCategories = response.body();

                    List<String> catNames = new ArrayList<>();
                    catNames.add("Все категории");
                    for (Category c : allCategories) catNames.add(c.getName());

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
        String searchText = "";
        TextInputEditText etSearch = findViewById(R.id.etSearch);
        if (etSearch != null && etSearch.getText() != null) {
            searchText = etSearch.getText().toString().toLowerCase().trim();
        }

        List<Crop> filteredList = new ArrayList<>();

        for (Crop item : allPlants) {
            boolean matchesSearch = item.getName() != null &&
                    item.getName().toLowerCase().contains(searchText);

            boolean matchesCategory = currentCategory.equals("Все категории") ||
                    (item.getCategory() != null && item.getCategory().equals(currentCategory));

            if (matchesSearch && matchesCategory) {
                filteredList.add(item);
            }
        }

        java.util.Collections.sort(filteredList, (o1, o2) -> {
            if (o1.getName() == null || o2.getName() == null) return 0;
            int res = o1.getName().compareToIgnoreCase(o2.getName());
            return isAscending ? res : -res;
        });

        adapter.updateData(filteredList);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) loadPlants();
    }
}