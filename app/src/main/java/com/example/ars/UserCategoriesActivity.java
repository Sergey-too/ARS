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

import com.example.ars.adapters.UserCategoryAdapter;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.User;
import com.example.ars.models.UserCategory;
import com.example.ars.utils.SharedPreferencesHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserCategoriesActivity extends AppCompatActivity {

    private RecyclerView rvCategories;
    private UserCategoryAdapter adapter;
    private List<UserCategory> categoryList = new ArrayList<>();
    private ApiService apiService;
    private EditText etSearch;
    private SharedPreferencesHelper prefsHelper;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_categories);

        prefsHelper = new SharedPreferencesHelper(this);
        User user = prefsHelper.getUser();
        if (user == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = user.getId();

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
        adapter = new UserCategoryAdapter(
                categoryList,
                category -> showCategoryDialog(category),
                category -> showDeleteConfirmation(category)
        );
        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        rvCategories.setAdapter(adapter);

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    filterCategories(s.toString().toLowerCase());
                }
            });
        }
    }

    private void filterCategories(String query) {
        if (query.isEmpty()) {
            loadCategories();
        } else {
            List<UserCategory> filtered = new ArrayList<>();
            for (UserCategory category : categoryList) {
                if (category.getName().toLowerCase().contains(query)) {
                    filtered.add(category);
                }
            }
            adapter.updateList(filtered);
        }
    }

    private void loadCategories() {
        apiService.getUserCategories(currentUserId).enqueue(new Callback<List<UserCategory>>() {
            @Override
            public void onResponse(Call<List<UserCategory>> call, Response<List<UserCategory>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categoryList.clear();
                    categoryList.addAll(response.body());
                    adapter.updateList(categoryList);
                } else {
                    Toast.makeText(UserCategoriesActivity.this, "Нет категорий", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<UserCategory>> call, Throwable t) {
                Toast.makeText(UserCategoriesActivity.this, "Ошибка загрузки: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCategoryDialog(UserCategory category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null);
        EditText input = view.findViewById(R.id.etInputName);

        if (category != null) {
            builder.setTitle("Редактировать категорию");
            input.setText(category.getName());
        } else {
            builder.setTitle("Новая категория");
        }

        builder.setView(view);
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                if (category == null) {
                    createCategory(name);
                } else {
                    category.setName(name);
                    updateCategory(category);
                }
            } else {
                Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void createCategory(String name) {
        UserCategory newCategory = new UserCategory();
        newCategory.setUserId(currentUserId);
        newCategory.setName(name);

        apiService.createUserCategory(newCategory).enqueue(new Callback<UserCategory>() {
            @Override
            public void onResponse(Call<UserCategory> call, Response<UserCategory> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(UserCategoriesActivity.this, "Категория создана", Toast.LENGTH_SHORT).show();
                    loadCategories();
                } else if (response.code() == 400) {
                    Toast.makeText(UserCategoriesActivity.this, "Такая категория уже существует", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(UserCategoriesActivity.this, "Ошибка создания категории", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserCategory> call, Throwable t) {
                Toast.makeText(UserCategoriesActivity.this, "Ошибка: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCategory(UserCategory category) {
        apiService.updateUserCategory(category.getId(), category).enqueue(new Callback<UserCategory>() {
            @Override
            public void onResponse(Call<UserCategory> call, Response<UserCategory> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(UserCategoriesActivity.this, "Категория обновлена", Toast.LENGTH_SHORT).show();
                    loadCategories();
                } else {
                    Toast.makeText(UserCategoriesActivity.this, "Ошибка обновления", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserCategory> call, Throwable t) {
                Toast.makeText(UserCategoriesActivity.this, "Ошибка: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmation(UserCategory category) {
        new AlertDialog.Builder(this)
                .setTitle("Удаление")
                .setMessage("Удалить категорию '" + category.getName() + "'? Растения в этой категории останутся без категории.")
                .setPositiveButton("Удалить", (dialog, which) -> deleteCategory(category))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteCategory(UserCategory category) {
        apiService.deleteUserCategory(category.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(UserCategoriesActivity.this, "Категория удалена", Toast.LENGTH_SHORT).show();
                    loadCategories();
                } else if (response.code() == 409) {
                    Toast.makeText(UserCategoriesActivity.this,
                            "Нельзя удалить: в категории есть растения!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(UserCategoriesActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(UserCategoriesActivity.this, "Ошибка: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}