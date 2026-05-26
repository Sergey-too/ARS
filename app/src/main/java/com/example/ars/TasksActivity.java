package com.example.ars;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ars.adapters.TaskAdapter;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.TaskItem;
import com.example.ars.utils.SharedPreferencesHelper;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TasksActivity extends AppCompatActivity {

    private ApiService apiService;
    private SharedPreferencesHelper prefsHelper;
    private TaskAdapter adapter;
    private RecyclerView rvTasks;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private AutoCompleteTextView actvCategoryFilter;
    private List<TaskItem> allTasks = new ArrayList<>();
    private List<TaskItem> filteredTasks = new ArrayList<>();
    private String selectedAction = "Все работы";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        apiService = RetrofitClient.getApiService();
        prefsHelper = new SharedPreferencesHelper(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvTasks = findViewById(R.id.rvTasks);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        actvCategoryFilter = findViewById(R.id.actvCategoryFilter);

        String[] actions = {"Все работы", "Посадка", "Полив", "Удобрение", "Уход за почвой", "Защита", "Сбор урожая"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, actions);
        actvCategoryFilter.setAdapter(spinnerAdapter);
        actvCategoryFilter.setOnItemClickListener((parent, view, position, id) -> {
            selectedAction = actions[position];
            applyFilter();
        });
        actvCategoryFilter.setText("Все работы", false);

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(filteredTasks, this::onTaskComplete);
        rvTasks.setAdapter(adapter);

        loadTasks();
    }

    private void applyFilter() {
        filteredTasks.clear();

        if (selectedAction.equals("Все работы")) {
            filteredTasks.addAll(allTasks);
        } else {
            for (TaskItem task : allTasks) {
                String actionName = getActionNameById(task.getActionTypeId());
                if (actionName.equals(selectedAction)) {
                    filteredTasks.add(task);
                }
            }
        }

        adapter.updateData(filteredTasks);

        if (filteredTasks.isEmpty() && allTasks.isEmpty()) {
            tvEmpty.setVisibility(android.view.View.VISIBLE);
            rvTasks.setVisibility(android.view.View.GONE);
            tvEmpty.setText("Нет задач на текущую неделю");
        } else if (filteredTasks.isEmpty()) {
            tvEmpty.setVisibility(android.view.View.VISIBLE);
            rvTasks.setVisibility(android.view.View.GONE);
            tvEmpty.setText("Нет задач по выбранному фильтру");
        } else {
            tvEmpty.setVisibility(android.view.View.GONE);
            rvTasks.setVisibility(android.view.View.VISIBLE);
        }
    }

    private String getActionNameById(Integer id) {
        if (id == null) return "Неизвестно";
        switch (id) {
            case 1: return "Посадка";
            case 2: return "Полив";
            case 3: return "Удобрение";
            case 4: return "Уход за почвой";
            case 5: return "Защита";
            case 6: return "Сбор урожая";
            default: return "Уход";
        }
    }

    private void loadTasks() {
        if (prefsHelper.getUser() == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(android.view.View.VISIBLE);
        tvEmpty.setVisibility(android.view.View.GONE);
        rvTasks.setVisibility(android.view.View.GONE);

        int userId = prefsHelper.getUser().getId();

        apiService.getWeeklyTasks(userId).enqueue(new Callback<List<TaskItem>>() {
            @Override
            public void onResponse(Call<List<TaskItem>> call, Response<List<TaskItem>> response) {
                progressBar.setVisibility(android.view.View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    allTasks.clear();
                    allTasks.addAll(response.body());
                    applyFilter();
                } else {
                    tvEmpty.setVisibility(android.view.View.VISIBLE);
                    tvEmpty.setText("Ошибка загрузки задач");
                    Toast.makeText(TasksActivity.this, "Ошибка загрузки задач", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<TaskItem>> call, Throwable t) {
                progressBar.setVisibility(android.view.View.GONE);
                tvEmpty.setVisibility(android.view.View.VISIBLE);
                tvEmpty.setText("Ошибка: " + t.getMessage());
                Toast.makeText(TasksActivity.this, "Ошибка: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onTaskComplete(TaskItem task) {
        progressBar.setVisibility(android.view.View.VISIBLE);

        java.util.Map<String, Object> request = new java.util.HashMap<>();
        request.put("cropName", task.getCropName());
        request.put("variety", task.getVariety());
        request.put("areaName", task.getAreaName());
        request.put("actionTypeId", task.getActionTypeId());

        apiService.completeTask(request).enqueue(new Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(Call<java.util.Map<String, Object>> call, Response<java.util.Map<String, Object>> response) {
                progressBar.setVisibility(android.view.View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(TasksActivity.this, "Задача выполнена!", Toast.LENGTH_SHORT).show();
                    loadTasks();
                } else {
                    Toast.makeText(TasksActivity.this, "Ошибка при выполнении", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<java.util.Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(android.view.View.GONE);
                Toast.makeText(TasksActivity.this, "Ошибка: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}