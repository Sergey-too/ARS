package com.example.ars;

import android.app.AlertDialog;
import android.content.SharedPreferences;
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
    private Set<String> completedTasksFromDb = new HashSet<>();

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
            finalizeAndFilterTasks();
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
                    finalizeAndFilterTasks();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<WeatherResponse> call, Throwable t) {
                weatherCache.put(regionId, new ArrayList<>());
                if (weatherCache.size() == totalExpected) {
                    adjustTasksByWeather(weatherCache);
                    finalizeAndFilterTasks();
                }
            }
        });
    }

    private void adjustTasksByWeather(Map<Integer, List<WeatherData>> weatherCache) {
        Iterator<TaskItem> iterator = allTasks.iterator();

        List<TaskItem> newWateringTasks = new ArrayList<>();

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

        addWeatherBasedWateringTasks(weatherCache, newWateringTasks);
        allTasks.addAll(newWateringTasks);
    }

    private void addWeatherBasedWateringTasks(Map<Integer, List<WeatherData>> weatherCache, List<TaskItem> newTasks) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());

        for (UserCrop uc : userCrops) {
            if (uc.getArea() == null || uc.getArea().getRegionId() == null) continue;

            int regionId = uc.getArea().getRegionId();
            List<WeatherData> weatherList = weatherCache.get(regionId);
            if (weatherList == null || weatherList.isEmpty()) continue;

            WeatherData todayWeather = null;
            for (WeatherData wd : weatherList) {
                if (wd.getDate() != null && wd.getDate().equals(today)) {
                    todayWeather = wd;
                    break;
                }
            }

            if (todayWeather == null) continue;

            boolean needsExtraWatering = false;
            String reason = "";

            double humidity = parseDouble(todayWeather.getHumidityMin());
            double tempMax = parseDouble(todayWeather.getTemperatureMax());

            Integer minHumidity = null;
            Integer maxTemp = null;

            if (uc.getCrop() != null) {
                minHumidity = uc.getCrop().getMinHumidity();
                maxTemp = uc.getCrop().getMaxTemp() != null ? uc.getCrop().getMaxTemp().intValue() : null;
            } else if (uc.getIndividualCrop() != null) {
                minHumidity = uc.getIndividualCrop().getMinHumidity();
                maxTemp = uc.getIndividualCrop().getMaxTemp() != null ? uc.getIndividualCrop().getMaxTemp().intValue() : null;
            }

            if (minHumidity != null && humidity < minHumidity) {
                needsExtraWatering = true;
                reason = "Низкая влажность (" + String.format("%.0f", humidity) + "%), требуется дополнительный полив";
            }
            else if (maxTemp != null && tempMax > maxTemp + 5) {
                needsExtraWatering = true;
                reason = "Сильная жара (" + String.format("%.0f", tempMax) + "°C), требуется частый полив";
            }
            else if (maxTemp != null && tempMax > maxTemp) {
                needsExtraWatering = true;
                reason = "Высокая температура (" + String.format("%.0f", tempMax) + "°C), рекомендуется полив";
            }

            if (needsExtraWatering) {
                TaskItem wateringTask = new TaskItem();
                wateringTask.setCropName(uc.getName());
                wateringTask.setVariety(uc.getCrop() != null ? uc.getCrop().getVariety() :
                        (uc.getIndividualCrop() != null ? uc.getIndividualCrop().getVariety() : null));
                wateringTask.setAreaName(uc.getArea().getName());
                wateringTask.setActionTypeId(2);
                wateringTask.setActionName("Полив");
                wateringTask.setDueDate(today);
                wateringTask.setLastDoneAt(null);
                if (uc.getGarden() != null) {
                    wateringTask.setGardenName(uc.getGarden().getName());
                }

                boolean alreadyExists = false;
                for (TaskItem existing : allTasks) {
                    if (existing.getCropName().equals(wateringTask.getCropName()) &&
                            existing.getActionTypeId() == 2 &&
                            existing.getDueDate().equals(today)) {
                        alreadyExists = true;
                        break;
                    }
                }
                if (!alreadyExists) {
                    newTasks.add(wateringTask);
                    Log.d("TasksActivity", "Добавлена задача на полив для " + uc.getName() + " - " + reason);
                }
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

    private void finalizeAndFilterTasks() {
        removeDuplicateTasks();
        filterCompletedFromDb();
        applyFilter();
    }

    private void removeDuplicateTasks() {
        Map<String, TaskItem> uniqueMap = new LinkedHashMap<>();
        for (TaskItem task : allTasks) {
            String key = task.getCropName() + "|" + task.getAreaName() + "|" + task.getDueDate() + "|" + task.getActionTypeId();
            if (!uniqueMap.containsKey(key)) {
                uniqueMap.put(key, task);
            }
        }
        allTasks.clear();
        allTasks.addAll(uniqueMap.values());
    }

    private void filterCompletedFromDb() {
        if (prefsHelper.getUser() == null) return;

        int userId = prefsHelper.getUser().getId();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (TaskItem task : allTasks) {
            String date = task.getDueDate();
            String cropName = task.getCropName();
            String variety = task.getVariety();
            String areaName = task.getAreaName();
            String gardenName = task.getGardenName();
            int actionTypeId = task.getActionTypeId();

            try {
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                        .build();

                String url = "http://192.168.100.9:8080/api/history/check-task?userId=" + userId +
                        "&cropName=" + java.net.URLEncoder.encode(cropName, "UTF-8") +
                        "&variety=" + java.net.URLEncoder.encode(variety != null ? variety : "", "UTF-8") +
                        "&areaName=" + java.net.URLEncoder.encode(areaName, "UTF-8") +
                        "&gardenName=" + java.net.URLEncoder.encode(gardenName != null ? gardenName : "", "UTF-8") +
                        "&actionTypeId=" + actionTypeId +
                        "&date=" + date;

                okhttp3.Request request = new okhttp3.Request.Builder().url(url).get().build();
                okhttp3.Response response = client.newCall(request).execute();
                String body = response.body().string();

                if (body.contains("true")) {
                    completedTasksFromDb.add(task.getCropName() + "|" + task.getAreaName() + "|" + task.getDueDate() + "|" + task.getActionTypeId());
                }
                response.close();
            } catch (Exception e) {
                Log.e("TasksActivity", "Ошибка проверки: " + e.getMessage());
            }
        }

        List<TaskItem> filtered = new ArrayList<>();
        for (TaskItem task : allTasks) {
            String key = task.getCropName() + "|" + task.getAreaName() + "|" + task.getDueDate() + "|" + task.getActionTypeId();
            if (!completedTasksFromDb.contains(key)) {
                filtered.add(task);
            }
        }
        allTasks.clear();
        allTasks.addAll(filtered);
    }

    private void applyFilter() {
        filteredTasks.clear();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date todayDate = cal.getTime();

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

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date todayDate = cal.getTime();

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
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (task.getDueDate() == null) {
            Toast.makeText(this, "Ошибка: у задачи нет даты выполнения", Toast.LENGTH_SHORT).show();
            return;
        }

        if (task.getDueDate().compareTo(today) > 0) {
            String formattedDate = formatDateForDisplay(task.getDueDate());
            Toast.makeText(this, "Эту задачу можно выполнить только " + formattedDate, Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        Map<String, Object> request = new HashMap<>();
        request.put("cropName", task.getCropName());
        request.put("variety", task.getVariety());
        request.put("areaName", task.getAreaName());
        request.put("actionTypeId", task.getActionTypeId());
        request.put("gardenName", task.getGardenName());
        request.put("userId", prefsHelper.getUser().getId());
        request.put("dueDate", task.getDueDate());

        apiService.completeTask(request).enqueue(new retrofit2.Callback<Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<Map<String, Object>> call, retrofit2.Response<Map<String, Object>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    Boolean success = (Boolean) response.body().get("success");
                    if (success != null && success) {
                        Toast.makeText(TasksActivity.this, "Задача выполнена!", Toast.LENGTH_SHORT).show();
                        loadAllData();
                    } else {
                        String error = (String) response.body().get("error");
                        Toast.makeText(TasksActivity.this, error != null ? error : "Ошибка", Toast.LENGTH_SHORT).show();
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

    private String formatDateForDisplay(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("d MMMM yyyy", new Locale("ru"));
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateStr;
        }
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
            showEmpty("Нет благоприятных дней для посадки или все растения уже посажены");
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

            if (recCalendar.getTime().after(today.getTime())) {
                new AlertDialog.Builder(this)
                        .setTitle("Нельзя посадить")
                        .setMessage("Нельзя посадить растение в будущем. Выберите сегодняшний или прошедший день.")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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