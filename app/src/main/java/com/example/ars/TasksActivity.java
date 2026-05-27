package com.example.ars;

import android.os.Bundle;
import android.view.View;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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

    private List<TaskItem> overdueTasks = new ArrayList<>();
    private List<TaskItem> todayTasks = new ArrayList<>();
    private List<TaskItem> futureTasks = new ArrayList<>();

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
        adapter = new TaskAdapter(this::onTaskComplete);
        rvTasks.setAdapter(adapter);

        loadTasks();
    }

    private void applyFilter() {
        overdueTasks.clear();
        todayTasks.clear();
        futureTasks.clear();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date todayDate = calendar.getTime();

        for (TaskItem task : allTasks) {
            boolean matchesAction = selectedAction.equals("Все работы") ||
                    getActionNameById(task.getActionTypeId()).equals(selectedAction);

            if (!matchesAction) continue;

            String taskDateStr = task.getDueDate();
            if (taskDateStr == null || taskDateStr.isEmpty()) {
                futureTasks.add(task);
                continue;
            }

            try {
                Date taskDate = sdf.parse(taskDateStr);
                if (taskDate != null) {
                    if (taskDate.before(todayDate)) {
                        overdueTasks.add(task);
                    } else if (taskDate.equals(todayDate)) {
                        todayTasks.add(task);
                    } else {
                        futureTasks.add(task);
                    }
                } else {
                    futureTasks.add(task);
                }
            } catch (Exception e) {
                futureTasks.add(task);
            }
        }

        adapter.setGroupedData(overdueTasks, todayTasks, futureTasks);

        if (overdueTasks.isEmpty() && todayTasks.isEmpty() && futureTasks.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvTasks.setVisibility(View.GONE);
            tvEmpty.setText("Нет задач на текущую неделю");
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvTasks.setVisibility(View.VISIBLE);
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