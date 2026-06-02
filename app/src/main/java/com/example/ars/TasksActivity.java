package com.example.ars;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.adapters.PlantingAdapter;
import com.example.ars.adapters.TaskAdapter;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.*;
import com.example.ars.utils.SharedPreferencesHelper;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TasksActivity extends AppCompatActivity {

    private ApiService apiService;
    private SharedPreferencesHelper prefsHelper;

    private TaskAdapter taskAdapter;
    private RecyclerView rvTasks;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private TextView tvTitle;
    private AutoCompleteTextView actvCategoryFilter;
    private List<TaskItem> allTasks = new ArrayList<>();
    private List<TaskItem> filteredTasks = new ArrayList<>();
    private String selectedAction = "Все работы";

    private PlantingAdapter plantingAdapter;
    private RecyclerView rvRecommendations;
    private List<UserCrop> userCrops = new ArrayList<>();
    private final Set<String> plantedKeys = new HashSet<>();
    private final Map<String, List<WeatherData>> weatherByRegion = new HashMap<>();

    private final Map<String, UserCrop> cropCache = new ConcurrentHashMap<>();

    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        apiService = RetrofitClient.getApiService();
        prefsHelper = new SharedPreferencesHelper(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        initViews();
        setupTabs();
        setupTaskViews();
        setupPlantingViews();

        loadAllData();
    }

    private void initViews() {
        rvTasks = findViewById(R.id.rvTasks);
        rvRecommendations = findViewById(R.id.rvRecommendations);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvTitle = findViewById(R.id.tvTitle);
        actvCategoryFilter = findViewById(R.id.actvCategoryFilter);
    }

    private void setupTabs() {
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                if (currentTab == 0) {
                    showTasksView();
                    loadAllData();
                } else {
                    showPlantingView();
                    loadPlantingData();
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupTaskViews() {
        String[] actions = {"Все работы", "Посадка", "Полив", "Удобрение", "Уход за почвой", "Защита", "Сбор урожая"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, actions);
        actvCategoryFilter.setAdapter(spinnerAdapter);
        actvCategoryFilter.setOnItemClickListener((parent, view, position, id) -> {
            selectedAction = actions[position];
            applyFilter();
        });
        actvCategoryFilter.setText("Все работы", false);

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(this::onTaskComplete);
        rvTasks.setAdapter(taskAdapter);
    }

    private void setupPlantingViews() {
        rvRecommendations.setLayoutManager(new LinearLayoutManager(this));
        plantingAdapter = new PlantingAdapter();
        plantingAdapter.setOnPlantClickListener((item, position) -> onPlantClick(item));
        rvRecommendations.setAdapter(plantingAdapter);
    }

    private void showTasksView() {
        tvTitle.setText("Задачи на неделю");
        rvTasks.setVisibility(View.VISIBLE);
        rvRecommendations.setVisibility(View.GONE);
        findViewById(R.id.tilCategory).setVisibility(View.VISIBLE);
    }

    private void showPlantingView() {
        tvTitle.setText("Рекомендации по посадке");
        rvTasks.setVisibility(View.GONE);
        rvRecommendations.setVisibility(View.VISIBLE);
        findViewById(R.id.tilCategory).setVisibility(View.GONE);
    }

    private void loadAllData() {
        User user = prefsHelper.getUser();
        if (user == null) {
            showEmpty("Пользователь не авторизован");
            return;
        }

        showLoading(true);

        apiService.getUserCrops(user.getId()).enqueue(new retrofit2.Callback<List<UserCrop>>() {
            @Override
            public void onResponse(retrofit2.Call<List<UserCrop>> call, retrofit2.Response<List<UserCrop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userCrops = response.body();
                    cropCache.clear();
                    for (UserCrop uc : userCrops) {
                        String cropName = uc.getName();
                        String areaName = uc.getArea() != null ? uc.getArea().getName() : "";
                        String key = (cropName + "|" + areaName).toLowerCase().trim();
                        cropCache.put(key, uc);
                    }
                }
                loadTasks();
            }

            @Override
            public void onFailure(retrofit2.Call<List<UserCrop>> call, Throwable t) {
                loadTasks();
            }
        });
    }

    private void loadTasks() {
        User user = prefsHelper.getUser();
        if (user == null) return;

        apiService.getWeeklyTasks(user.getId()).enqueue(new retrofit2.Callback<List<TaskItem>>() {
            @Override
            public void onResponse(retrofit2.Call<List<TaskItem>> call, retrofit2.Response<List<TaskItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allTasks.clear();
                    allTasks.addAll(response.body());
                    loadWeatherAndAdjustTasks();
                } else {
                    showLoading(false);
                    showEmpty("Ошибка загрузки задач");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<TaskItem>> call, Throwable t) {
                showLoading(false);
                showEmpty("Ошибка: " + t.getMessage());
            }
        });
    }

    private void loadWeatherAndAdjustTasks() {
        Set<Integer> regionIds = new HashSet<>();
        for (TaskItem task : allTasks) {
            if (task.getActionTypeId() != null && task.getActionTypeId() == 2) {
                String key = (task.getCropName() + "|" + task.getAreaName()).toLowerCase().trim();
                UserCrop uc = cropCache.get(key);
                if (uc != null && uc.getArea() != null && uc.getArea().getRegionId() != null) {
                    regionIds.add(uc.getArea().getRegionId());
                }
            }
        }

        if (regionIds.isEmpty()) {
            applyFilter();
            return;
        }

        Map<Integer, List<WeatherData>> weatherCache = new HashMap<>();
        for (Integer regionId : regionIds) {
            loadWeatherForRegion(regionId, weatherCache, regionIds.size());
        }
    }

    private void loadWeatherForRegion(Integer regionId, Map<Integer, List<WeatherData>> weatherCache, int totalExpected) {
        apiService.getWeatherByRegionId(regionId).enqueue(new retrofit2.Callback<WeatherResponse>() {
            @Override
            public void onResponse(retrofit2.Call<WeatherResponse> call, retrofit2.Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getWeather() != null) {
                    weatherCache.put(regionId, response.body().getWeather());
                }
                if (weatherCache.size() == totalExpected) {
                    adjustTasksByWeather(weatherCache);
                    applyFilter();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<WeatherResponse> call, Throwable t) {
                weatherCache.put(regionId, new ArrayList<>());
                if (weatherCache.size() == totalExpected) {
                    adjustTasksByWeather(weatherCache);
                    applyFilter();
                }
            }
        });
    }

    private void adjustTasksByWeather(Map<Integer, List<WeatherData>> weatherCache) {
        Iterator<TaskItem> iterator = allTasks.iterator();

        while (iterator.hasNext()) {
            TaskItem task = iterator.next();

            String key = (task.getCropName() + "|" + task.getAreaName()).toLowerCase().trim();
            UserCrop uc = cropCache.get(key);
            if (uc != null && uc.getGarden() != null) {
                task.setGardenName(uc.getGarden().getName());
            }

            if (task.getActionTypeId() == null || task.getActionTypeId() != 2) {
                continue;
            }

            if (!shouldWater(task, weatherCache)) {
                iterator.remove();
            }
        }
    }

    private boolean shouldWater(TaskItem task, Map<Integer, List<WeatherData>> weatherCache) {
        String key = (task.getCropName() + "|" + task.getAreaName()).toLowerCase().trim();
        UserCrop uc = cropCache.get(key);

        if (uc == null) return true;

        Integer daysToGermination = null;
        if (uc.getCrop() != null) {
            daysToGermination = uc.getCrop().getDaysToGermination();
        } else if (uc.getIndividualCrop() != null) {
            daysToGermination = uc.getIndividualCrop().getDaysToGermination();
        }

        if (uc.getPlantedAt() != null && daysToGermination != null && daysToGermination > 0) {
            try {
                LocalDate plantedDate = LocalDate.parse(uc.getPlantedAt(), DateTimeFormatter.ISO_LOCAL_DATE);
                long daysSincePlanted = ChronoUnit.DAYS.between(plantedDate, LocalDate.now());
                if (daysSincePlanted < daysToGermination) {
                    return false;
                }
            } catch (Exception e) {
                // ignore
            }
        }

        if (uc.getArea() == null || uc.getArea().getRegionId() == null) return true;

        List<WeatherData> weatherList = weatherCache.get(uc.getArea().getRegionId());
        if (weatherList == null || weatherList.isEmpty()) return true;

        for (WeatherData wd : weatherList) {
            if (wd.getDate() != null && wd.getDate().equals(task.getDueDate())) {
                double precipitation = parseDouble(wd.getPrecipitation());
                if (precipitation >= 5) {
                    return false;
                }
                break;
            }
        }

        return true;
    }

    private void applyFilter() {
        filteredTasks.clear();

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
            if (taskDateStr == null || taskDateStr.isEmpty()) continue;

            try {
                Date taskDate = sdf.parse(taskDateStr);
                if (taskDate != null && (taskDate.equals(todayDate) || taskDate.after(todayDate))) {
                    filteredTasks.add(task);
                }
            } catch (Exception e) {
                // ignore
            }
        }

        groupTasksAndUpdate();

        if (filteredTasks.isEmpty()) {
            showEmpty("Нет задач на текущую неделю");
        } else {
            hideEmpty();
        }

        showLoading(false);
    }

    private void groupTasksAndUpdate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date todayDate = new Date();

        List<TaskItem> overdue = new ArrayList<>();
        List<TaskItem> today = new ArrayList<>();
        List<TaskItem> future = new ArrayList<>();

        for (TaskItem task : filteredTasks) {
            try {
                Date taskDate = sdf.parse(task.getDueDate());
                if (taskDate != null) {
                    if (taskDate.before(todayDate)) {
                        overdue.add(task);
                    } else if (dateEquals(taskDate, todayDate)) {
                        today.add(task);
                    } else {
                        future.add(task);
                    }
                } else {
                    future.add(task);
                }
            } catch (Exception e) {
                future.add(task);
            }
        }

        taskAdapter.setGroupedData(overdue, today, future);
    }

    private boolean dateEquals(Date d1, Date d2) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(d1).equals(sdf.format(d2));
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

    private void onTaskComplete(TaskItem task) {
        showLoading(true);

        Map<String, Object> request = new HashMap<>();
        request.put("cropName", task.getCropName());
        request.put("variety", task.getVariety());
        request.put("areaName", task.getAreaName());
        request.put("actionTypeId", task.getActionTypeId());

        apiService.completeTask(request).enqueue(new retrofit2.Callback<Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<Map<String, Object>> call, retrofit2.Response<Map<String, Object>> response) {
                showLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(TasksActivity.this, "Задача выполнена!", Toast.LENGTH_SHORT).show();
                    loadAllData();
                } else {
                    Toast.makeText(TasksActivity.this, "Ошибка", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<Map<String, Object>> call, Throwable t) {
                showLoading(false);
                Toast.makeText(TasksActivity.this, "Ошибка: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPlantingData() {
        User currentUser = prefsHelper.getUser();
        if (currentUser == null) {
            showEmpty("Пользователь не авторизован");
            return;
        }

        showLoading(true);

        apiService.getUserCrops(currentUser.getId()).enqueue(new retrofit2.Callback<List<UserCrop>>() {
            @Override
            public void onResponse(retrofit2.Call<List<UserCrop>> call, retrofit2.Response<List<UserCrop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userCrops = response.body();
                    loadPlantedHistory();
                } else {
                    showLoading(false);
                    showEmpty("У вас нет растений");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<UserCrop>> call, Throwable t) {
                showLoading(false);
                showEmpty("Ошибка: " + t.getMessage());
            }
        });
    }

    private void loadPlantedHistory() {
        User currentUser = prefsHelper.getUser();
        if (currentUser == null) return;

        apiService.getPlantingHistory(currentUser.getId()).enqueue(new retrofit2.Callback<List<GardenHistory>>() {
            @Override
            public void onResponse(retrofit2.Call<List<GardenHistory>> call, retrofit2.Response<List<GardenHistory>> response) {
                plantedKeys.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (GardenHistory h : response.body()) {
                        if (h.getActionTypeId() != null && h.getActionTypeId() == 1 && h.getCropName() != null && h.getAreaName() != null) {
                            String key = h.getCropName().trim().toLowerCase() + "|" +
                                    (h.getVariety() != null ? h.getVariety().trim().toLowerCase() : "обычный") + "|" +
                                    h.getAreaName().trim().toLowerCase();
                            plantedKeys.add(key);
                        }
                    }
                }
                loadWeatherForRecommendations();
            }

            @Override
            public void onFailure(retrofit2.Call<List<GardenHistory>> call, Throwable t) {
                loadWeatherForRecommendations();
            }
        });
    }

    private void loadWeatherForRecommendations() {
        weatherByRegion.clear();
        Set<Integer> regionIds = new HashSet<>();
        for (UserCrop uc : userCrops) {
            if (uc.getArea() != null && uc.getArea().getRegionId() != null) {
                regionIds.add(uc.getArea().getRegionId());
            }
        }

        if (regionIds.isEmpty()) {
            showLoading(false);
            showEmpty("У ваших участков не указан регион");
            return;
        }

        for (Integer regionId : regionIds) {
            loadWeatherForRecommendationRegion(regionId, regionIds.size());
        }
    }

    private void loadWeatherForRecommendationRegion(Integer regionId, int totalExpected) {
        apiService.getWeatherByRegionId(regionId).enqueue(new retrofit2.Callback<WeatherResponse>() {
            @Override
            public void onResponse(retrofit2.Call<WeatherResponse> call, retrofit2.Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getWeather() != null) {
                    weatherByRegion.put(String.valueOf(regionId), response.body().getWeather());
                }
                if (weatherByRegion.size() == totalExpected) {
                    analyzeRecommendations();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<WeatherResponse> call, Throwable t) {
                weatherByRegion.put(String.valueOf(regionId), new ArrayList<>());
                if (weatherByRegion.size() == totalExpected) {
                    analyzeRecommendations();
                }
            }
        });
    }

    private void analyzeRecommendations() {
        List<PlantingRecommendation> recommendations = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM", new Locale("ru"));
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("ru"));
        Calendar calendar = Calendar.getInstance();

        for (int day = 0; day < 7; day++) {
            String dateStr = sdf.format(calendar.getTime());
            String displayDate = dateFormat.format(calendar.getTime());
            String dayOfWeek = dayFormat.format(calendar.getTime());

            for (UserCrop uc : userCrops) {
                if (uc.getArea() == null || uc.getArea().getRegionId() == null) continue;

                String key = getCropKey(uc);
                if (plantedKeys.contains(key)) continue;

                List<WeatherData> weatherList = weatherByRegion.get(String.valueOf(uc.getArea().getRegionId()));
                if (weatherList == null) continue;

                for (WeatherData wd : weatherList) {
                    if (wd.getDate() != null && wd.getDate().equals(dateStr)) {
                        PlantingRecommendation rec = createRecommendation(uc, wd, displayDate, dayOfWeek);
                        recommendations.add(rec);
                        break;
                    }
                }
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        finalizeRecommendations(recommendations);
    }

    private String getCropKey(UserCrop uc) {
        String cropName = uc.getName();
        String variety = uc.getCrop() != null ? uc.getCrop().getVariety() :
                (uc.getIndividualCrop() != null ? uc.getIndividualCrop().getVariety() : null);
        String areaName = uc.getArea().getName();
        return cropName.trim().toLowerCase() + "|" +
                (variety != null ? variety.trim().toLowerCase() : "обычный") + "|" +
                areaName.trim().toLowerCase();
    }

    private PlantingRecommendation createRecommendation(UserCrop uc, WeatherData wd, String date, String dayOfWeek) {
        PlantingRecommendation rec = new PlantingRecommendation();
        rec.setUserCropId(uc.getId());
        rec.setDate(date);
        rec.setDayOfWeek(dayOfWeek);
        rec.setAreaName(uc.getArea().getName());
        rec.setAreaId(uc.getAreaId());

        if (uc.getGarden() != null) {
            rec.setGardenName(uc.getGarden().getName());
        } else {
            rec.setGardenName("");
        }

        double tempMin = parseDouble(wd.getTemperatureMin());
        double tempMax = parseDouble(wd.getTemperatureMax());

        rec.setCropName(uc.getName());
        rec.setVariety(uc.getCrop() != null ? uc.getCrop().getVariety() :
                (uc.getIndividualCrop() != null ? uc.getIndividualCrop().getVariety() : null));

        rec.setTempCurrent(String.format("%.0f...%.0f°C", tempMin, tempMax));
        rec.setWeatherText((int)tempMin + "-" + (int)tempMax + "°C");

        return rec;
    }

    private void finalizeRecommendations(List<PlantingRecommendation> recommendations) {
        runOnUiThread(() -> {
            showLoading(false);
            if (recommendations.isEmpty()) {
                showEmpty("Нет подходящих дней для посадки");
            } else {
                hideEmpty();
                plantingAdapter.updateData(recommendations);
            }
        });
    }

    private void onPlantClick(PlantingRecommendation item) {
        try {
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM", new Locale("ru"));
            Date recDate = sdf.parse(item.getDate());

            Calendar recCalendar = Calendar.getInstance();
            recCalendar.setTime(recDate);
            recCalendar.set(Calendar.YEAR, today.get(Calendar.YEAR));
            recCalendar.set(Calendar.HOUR_OF_DAY, 0);
            recCalendar.set(Calendar.MINUTE, 0);
            recCalendar.set(Calendar.SECOND, 0);
            recCalendar.set(Calendar.MILLISECOND, 0);

            Log.d("DATE_CHECK", "Сегодня: " + today.getTime());
            Log.d("DATE_CHECK", "Дата посадки: " + recCalendar.getTime());
            Log.d("DATE_CHECK", "after: " + recCalendar.getTime().after(today.getTime()));

            if (recCalendar.getTime().after(today.getTime())) {
                Log.d("DATE_CHECK", "Показываем диалог ошибки");
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("Нельзя посадить")
                        .setMessage("Нельзя посадить растение в будущем. Выберите сегодняшний или прошедший день.")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }
        } catch (Exception e) {
            Log.e("DATE_CHECK", "Ошибка: " + e.getMessage());
            e.printStackTrace();
        }

        Log.d("DATE_CHECK", "Показываем диалог посадки");
        new AlertDialog.Builder(this)
                .setTitle("Посадка")
                .setMessage("Посадить " + item.getCropName() + " на участке " + item.getAreaName() + "?")
                .setPositiveButton("Да", (d, w) -> performPlanting(item))
                .setNegativeButton("Нет", null)
                .show();
    }

    private void performPlanting(PlantingRecommendation item) {
        User currentUser = prefsHelper.getUser();
        if (currentUser == null) return;

        showLoading(true);

        Map<String, Object> request = new HashMap<>();
        request.put("userCropId", item.getUserCropId());
        request.put("areaId", item.getAreaId());
        request.put("actionTypeId", 1);

        apiService.plantCrop(request).enqueue(new retrofit2.Callback<Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<Map<String, Object>> call, retrofit2.Response<Map<String, Object>> response) {
                showLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(TasksActivity.this, "Растение посажено!", Toast.LENGTH_SHORT).show();
                    if (currentTab == 1) {
                        loadPlantingData();
                    }
                } else {
                    Toast.makeText(TasksActivity.this, "Ошибка: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<Map<String, Object>> call, Throwable t) {
                showLoading(false);
                Toast.makeText(TasksActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) tvEmpty.setVisibility(View.GONE);
    }

    private void showEmpty(String message) {
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(message);
        if (currentTab == 0) {
            rvTasks.setVisibility(View.GONE);
        } else {
            rvRecommendations.setVisibility(View.GONE);
        }
    }

    private void hideEmpty() {
        tvEmpty.setVisibility(View.GONE);
        if (currentTab == 0) {
            rvTasks.setVisibility(View.VISIBLE);
        } else {
            rvRecommendations.setVisibility(View.VISIBLE);
        }
    }
}