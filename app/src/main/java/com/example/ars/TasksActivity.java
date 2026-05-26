package com.example.ars;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ars.adapters.TasksAdapter;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.TaskItem;
import com.example.ars.utils.SharedPreferencesHelper;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TasksActivity extends AppCompatActivity {

    private ApiService apiService;
    private SharedPreferencesHelper prefsHelper;
    private TasksAdapter adapter;
    private RecyclerView rvTasks;
    private CircularProgressIndicator progressBar;
    private List<TaskItem> tasks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        apiService = RetrofitClient.getApiService();
        prefsHelper = new SharedPreferencesHelper(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvTasks = findViewById(R.id.rvTasks);
        progressBar = findViewById(R.id.progressBar);

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TasksAdapter(tasks, this::completeTask);
        rvTasks.setAdapter(adapter);

        loadTasks();
    }

    private void loadTasks() {
        if (prefsHelper.getUser() == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(android.view.View.VISIBLE);
        int userId = prefsHelper.getUser().getId();

        apiService.getWeeklyTasks(userId).enqueue(new Callback<List<TaskItem>>() {
            @Override
            public void onResponse(Call<List<TaskItem>> call, Response<List<TaskItem>> response) {
                progressBar.setVisibility(android.view.View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    tasks.clear();
                    tasks.addAll(response.body());
                    adapter.updateList(tasks);

                    if (tasks.isEmpty()) {
                        Toast.makeText(TasksActivity.this, "Нет задач на ближайшую неделю", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(TasksActivity.this, "Ошибка загрузки задач", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<TaskItem>> call, Throwable t) {
                progressBar.setVisibility(android.view.View.GONE);
                Toast.makeText(TasksActivity.this, "Ошибка: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void completeTask(TaskItem task, int position) {
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
                    tasks.remove(position);
                    adapter.updateList(tasks);
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