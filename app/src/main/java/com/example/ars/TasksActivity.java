package com.example.ars;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.adapters.TaskAdapter;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.TaskItem;
import com.example.ars.utils.SharedPreferencesHelper;

import java.text.SimpleDateFormat;
import java.util.*;

public class TasksActivity extends AppCompatActivity {

    private ApiService apiService;
    private SharedPreferencesHelper prefsHelper;
    private TaskAdapter adapter;
    private List<TaskItem> tasks = new ArrayList<>();

    private TextView tvEmpty;
    private RecyclerView rvTasks;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        prefsHelper = new SharedPreferencesHelper(this);
        apiService = RetrofitClient.getApiService();

        tvEmpty = findViewById(R.id.tvEmpty);
        rvTasks = findViewById(R.id.rvTasks);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(tasks, this::onTaskComplete);
        rvTasks.setAdapter(adapter);

        loadTasks();
    }

    private void loadTasks() {
        if (prefsHelper.getUser() == null) return;
        showLoading(true);

        apiService.getWeeklyTasks(prefsHelper.getUser().getId()).enqueue(new retrofit2.Callback<List<TaskItem>>() {
            @Override
            public void onResponse(retrofit2.Call<List<TaskItem>> call, retrofit2.Response<List<TaskItem>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    tasks = response.body();
                    adapter.updateData(tasks);
                    tvEmpty.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<TaskItem>> call, Throwable t) {
                showLoading(false);
                Toast.makeText(TasksActivity.this, "Ошибка загрузки", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onTaskComplete(TaskItem task) {
        showLoading(true);

        Map<String, Object> request = new HashMap<>();
        request.put("cropName", task.getCropName());
        request.put("variety", task.getVariety());
        request.put("areaName", task.getAreaName());
        request.put("actionTypeId", task.getActionTypeId());
        request.put("doneAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date()));

        apiService.completeTask(request).enqueue(new retrofit2.Callback<Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<Map<String, Object>> call, retrofit2.Response<Map<String, Object>> response) {
                showLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(TasksActivity.this, "Выполнено!", Toast.LENGTH_SHORT).show();
                    loadTasks();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<Map<String, Object>> call, Throwable t) {
                showLoading(false);
                Toast.makeText(TasksActivity.this, "Ошибка выполнения", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}