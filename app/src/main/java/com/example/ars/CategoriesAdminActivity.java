package com.example.ars;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.adapters.CategoryAdapter;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.Category;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoriesAdminActivity extends AppCompatActivity {

    private RecyclerView rvCategories;
    private CategoryAdapter adapter;
    private List<Category> categoryList = new ArrayList<>();
    private List<Category> filteredList = new ArrayList<>();
    private ApiService apiService;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories_admin);

        initViews();
        setupRetrofit();
        setupRecyclerView();
        loadCategories();

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        FloatingActionButton fabAdd = findViewById(R.id.fabAddCategory);
        fabAdd.setOnClickListener(v -> showCategoryDialog(null));
    }

    private void initViews() {
        rvCategories = findViewById(R.id.rvCategories);
        etSearch = findViewById(R.id.etSearch);
    }

    private void setupRetrofit() {
        apiService = RetrofitClient.getApiService();
    }

    private void setupRecyclerView() {
        adapter = new CategoryAdapter(
                filteredList,
                category -> showCategoryDialog(category),
                category -> showDeleteConfirmation(category)
        );
        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        rvCategories.setAdapter(adapter);
    }

    private void loadCategories() {
        apiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categoryList.clear();
                    categoryList.addAll(response.body());
                    filteredList.clear();
                    filteredList.addAll(categoryList);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Toast.makeText(CategoriesAdminActivity.this, "Ошибка загрузки", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCategoryDialog(Category category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null);
        EditText input = view.findViewById(R.id.etInputName);

        if (category != null) {
            builder.setTitle("Редактирование");
            input.setText(category.getName());
        } else {
            builder.setTitle("Новая категория");
        }

        builder.setView(view);
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                if (category == null) {
                    createCategory(new Category(name));
                } else {
                    category.setName(name);
                    updateCategory(category);
                }
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void createCategory(Category category) {
        apiService.createCategory(category).enqueue(new Callback<Category>() {
            @Override
            public void onResponse(Call<Category> call, Response<Category> response) {
                if (response.isSuccessful()) {
                    loadCategories();
                }
            }

            @Override
            public void onFailure(Call<Category> call, Throwable t) {
                Toast.makeText(CategoriesAdminActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCategory(Category category) {
        apiService.updateCategory(Math.toIntExact(category.getId()), category).enqueue(new Callback<Category>() {
            @Override
            public void onResponse(Call<Category> call, Response<Category> response) {
                if (response.isSuccessful()) {
                    loadCategories();
                }
            }

            @Override
            public void onFailure(Call<Category> call, Throwable t) {
                Toast.makeText(CategoriesAdminActivity.this, "Ошибка обновления", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmation(Category category) {
        new AlertDialog.Builder(this)
                .setTitle("Удаление")
                .setMessage("Удалить категорию '" + category.getName() + "'?")
                .setPositiveButton("Удалить", (dialog, which) -> deleteCategory(category))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteCategory(Category category) {
        apiService.deleteCategory(Math.toIntExact(category.getId())).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CategoriesAdminActivity.this, "Удалено", Toast.LENGTH_SHORT).show();
                    loadCategories();
                } else if (response.code() == 409) {
                    Toast.makeText(CategoriesAdminActivity.this,
                            "Нельзя удалить: в категории есть растения!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(CategoriesAdminActivity.this, "Ошибка связи", Toast.LENGTH_SHORT).show();
            }
        });
    }
}