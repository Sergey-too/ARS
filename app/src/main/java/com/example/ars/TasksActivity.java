package com.example.ars;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.adapters.TaskAdapter;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.ActionType;
import com.example.ars.models.TaskItem;
import com.example.ars.utils.SharedPreferencesHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TasksActivity extends AppCompatActivity {

    private ApiService apiService;
    private SharedPreferencesHelper prefsHelper;
    private TaskAdapter adapter;

    private List<TaskItem> allTasks = new ArrayList<>();
    private List<TaskItem> filteredTasks = new ArrayList<>();
    private Map<Integer, String> actionTypesLookup = new HashMap<>();
    private List<String> categoryNamesList = new ArrayList<>();
    private List<Integer> categoryIdsList = new ArrayList<>();
    private ArrayAdapter<String> dropdownAdapter;

    private TextView tvEmpty;
    private RecyclerView rvTasks;
    private ProgressBar progressBar;
    private AutoCompleteTextView actvCategoryFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        prefsHelper = new SharedPreferencesHelper(this);
        apiService = RetrofitClient.getApiService();

        tvEmpty = findViewById(R.id.tvEmpty);
        rvTasks = findViewById(R.id.rvTasks);
        progressBar = findViewById(R.id.progressBar);
        actvCategoryFilter = findViewById(R.id.actvCategoryFilter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(filteredTasks, this::onTaskComplete);
        rvTasks.setAdapter(adapter);

        dropdownAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categoryNamesList);
        actvCategoryFilter.setAdapter(dropdownAdapter);

        actvCategoryFilter.setOnItemClickListener((parent, view, position, id) -> {
            int selectedActionId = categoryIdsList.get(position);
            filterTasksByActionType(selectedActionId);
        });

        loadData();
    }

    private void loadData() {
        showLoading(true);

        apiService.getActionTypes().enqueue(new retrofit2.Callback<List<ActionType>>() {
            @Override
            public void onResponse(retrofit2.Call<List<ActionType>> call, retrofit2.Response<List<ActionType>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    actionTypesLookup.clear();
                    for (ActionType type : response.body()) {
                        actionTypesLookup.put(type.getId(), type.getName());
                    }
                }

                fetchWeeklyTasks();
            }

            @Override
            public void onFailure(retrofit2.Call<List<ActionType>> call, Throwable t) {
                fetchWeeklyTasks();
            }
        });
    }

    private void fetchWeeklyTasks() {
        if (prefsHelper.getUser() == null) {
            showLoading(false);
            return;
        }

        apiService.getWeeklyTasks(prefsHelper.getUser().getId()).enqueue(new retrofit2.Callback<List<TaskItem>>() {
            @Override
            public void onResponse(retrofit2.Call<List<TaskItem>> call, retrofit2.Response<List<TaskItem>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    allTasks = response.body();

                    setupDropdownFilter();
                    filterTasksByActionType(-1);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<TaskItem>> call, Throwable t) {
                showLoading(false);
                Toast.makeText(TasksActivity.this, "Ошибка загрузки задач", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDropdownFilter() {
        categoryNamesList.clear();
        categoryIdsList.clear();

        categoryNamesList.add("Все категории");
        categoryIdsList.add(-1);

        Set<Integer> uniqueActionIds = new HashSet<>();
        for (TaskItem task : allTasks) {
            uniqueActionIds.add(task.getActionTypeId());
        }

        for (Integer actionId : uniqueActionIds) {
            categoryNamesList.add(getCategoryNameById(actionId));
            categoryIdsList.add(actionId);
        }

        dropdownAdapter.notifyDataSetChanged();

        if (!categoryNamesList.isEmpty()) {
            actvCategoryFilter.setText(categoryNamesList.get(0), false);
        }
    }

    private void filterTasksByActionType(int actionTypeId) {
        filteredTasks.clear();
        if (actionTypeId == -1) {
            filteredTasks.addAll(allTasks);
        } else {
            for (TaskItem task : allTasks) {
                if (task.getActionTypeId() == actionTypeId) {
                    filteredTasks.add(task);
                }
            }
        }
        adapter.updateData(filteredTasks);
        tvEmpty.setVisibility(filteredTasks.isEmpty() ? View.VISIBLE : View.GONE);
    }

    public String getCategoryNameById(int actionId) {
        if (actionTypesLookup.containsKey(actionId)) {
            return actionTypesLookup.get(actionId);
        }
        return "Работа #" + actionId;
    }

    private void onTaskComplete(TaskItem task) {
        if (!isTaskAvailable(task)) {
            Toast.makeText(this, "Нельзя выполнить задачу раньше " + task.getDueDate(), Toast.LENGTH_LONG).show();
            return;
        }

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
                    loadData();
                }
            }
            @Override
            public void onFailure(retrofit2.Call<Map<String, Object>> call, Throwable t) {
                showLoading(false);
                Toast.makeText(TasksActivity.this, "Ошибка выполнения", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isTaskAvailable(TaskItem task) {
        if (task.getDueDate() == null) return true;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date dueDate = sdf.parse(task.getDueDate());
            Date today = sdf.parse(sdf.format(new Date()));
            return !dueDate.after(today);
        } catch (ParseException e) {
            return true;
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}