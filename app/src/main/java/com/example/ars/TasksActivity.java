package com.example.ars;

import android.app.AlertDialog;
import android.os.Bundle;
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
import java.util.*;

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

    private int currentTab = 0;
    private TabLayout tabTaskFilter;
    private int currentTaskFilter = 0;

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
        tabTaskFilter = findViewById(R.id.tabTaskFilter);

        setupTaskFilterTabs();
    }

    private void setupTaskFilterTabs() {
        if (tabTaskFilter != null) {
            tabTaskFilter.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    currentTaskFilter = tab.getPosition();
                    applyFilter();
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}

                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });
        }
    }

    private void setupTabs() {
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                if (currentTab == 0) {
                    showTasksView();
                    tabTaskFilter.setVisibility(View.VISIBLE);
                    loadAllData();
                } else {
                    showPlantingView();
                    tabTaskFilter.setVisibility(View.GONE);
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

        apiService.getWeeklyTasks(user.getId()).enqueue(new retrofit2.Callback<List<TaskItem>>() {
            @Override
            public void onResponse(retrofit2.Call<List<TaskItem>> call, retrofit2.Response<List<TaskItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allTasks.clear();
                    allTasks.addAll(response.body());
                    finalizeAndFilterTasks();
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

    private void finalizeAndFilterTasks() {
        removeDuplicateTasks();
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
                if (taskDate != null) {
                    if (currentTaskFilter == 0) {
                        if (!taskDate.after(todayDate)) {
                            filteredTasks.add(task);
                        }
                    } else {
                        if (taskDate.after(todayDate)) {
                            filteredTasks.add(task);
                        }
                    }
                }
            } catch (Exception e) {
            }
        }

        groupTasksAndUpdate();

        if (filteredTasks.isEmpty()) {
            showEmpty(currentTaskFilter == 0 ? "Нет текущих задач" : "Нет будущих задач");
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
                analyzeRecommendations();
            }

            @Override
            public void onFailure(retrofit2.Call<List<GardenHistory>> call, Throwable t) {
                analyzeRecommendations();
            }
        });
    }

    private void analyzeRecommendations() {
        List<PlantingRecommendation> recommendations = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM", new Locale("ru"));
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("ru"));
        Calendar calendar = Calendar.getInstance();

        Map<Integer, List<WeatherData>> weatherByRegion = new HashMap<>();
        Set<Integer> regionIds = new HashSet<>();

        for (UserCrop uc : userCrops) {
            if (uc.getArea() != null && uc.getArea().getRegionId() != null) {
                regionIds.add(uc.getArea().getRegionId());
            }
        }

        for (Integer regionId : regionIds) {
            loadWeatherForRecommendation(regionId, weatherByRegion, regionIds.size(), () -> {
                List<PlantingRecommendation> finalRecs = buildRecommendations(
                        userCrops, plantedKeys, weatherByRegion,
                        sdf, dateFormat, dayFormat, calendar, 6
                );
                finalizeRecommendations(finalRecs);
            });
        }

        if (regionIds.isEmpty()) {
            List<PlantingRecommendation> finalRecs = buildRecommendations(
                    userCrops, plantedKeys, weatherByRegion,
                    sdf, dateFormat, dayFormat, calendar, 6
            );
            finalizeRecommendations(finalRecs);
        }
    }

    private void loadWeatherForRecommendation(Integer regionId, Map<Integer, List<WeatherData>> weatherCache,
                                              int totalExpected, Runnable callback) {
        apiService.getWeatherByRegionId(regionId).enqueue(new retrofit2.Callback<WeatherResponse>() {
            @Override
            public void onResponse(retrofit2.Call<WeatherResponse> call, retrofit2.Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getWeather() != null) {
                    weatherCache.put(regionId, response.body().getWeather());
                }
                if (weatherCache.size() == totalExpected) {
                    callback.run();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<WeatherResponse> call, Throwable t) {
                weatherCache.put(regionId, new ArrayList<>());
                if (weatherCache.size() == totalExpected) {
                    callback.run();
                }
            }
        });
    }

    private List<PlantingRecommendation> buildRecommendations(
            List<UserCrop> userCrops,
            Set<String> plantedKeys,
            Map<Integer, List<WeatherData>> weatherByRegion,
            SimpleDateFormat sdf,
            SimpleDateFormat dateFormat,
            SimpleDateFormat dayFormat,
            Calendar calendar,
            int daysCount) {

        List<PlantingRecommendation> result = new ArrayList<>();

        for (int day = 0; day < daysCount; day++) {
            String dateStr = sdf.format(calendar.getTime());
            String displayDate = dateFormat.format(calendar.getTime());
            String dayOfWeek = dayFormat.format(calendar.getTime());
            if (dayOfWeek.length() > 0) {
                dayOfWeek = dayOfWeek.substring(0, 1).toUpperCase() + dayOfWeek.substring(1);
            }

            for (UserCrop uc : userCrops) {
                if (uc.getArea() == null) continue;

                String key = getCropKey(uc);
                if (plantedKeys.contains(key)) continue;

                PlantingRecommendation rec = new PlantingRecommendation();
                rec.setUserCropId(uc.getId());
                rec.setDate(displayDate);
                rec.setDayOfWeek(dayOfWeek);
                rec.setAreaName(uc.getArea().getName());
                rec.setAreaId(uc.getAreaId());
                rec.setCropName(uc.getName());
                rec.setVariety(uc.getCrop() != null ? uc.getCrop().getVariety() :
                        (uc.getIndividualCrop() != null ? uc.getIndividualCrop().getVariety() : null));

                if (uc.getGarden() != null) {
                    rec.setGardenName(uc.getGarden().getName());
                }

                if (uc.getCrop() != null) {
                    setRequiredValues(rec, uc.getCrop());
                } else if (uc.getIndividualCrop() != null) {
                    setRequiredValues(rec, uc.getIndividualCrop());
                }

                Integer regionId = uc.getArea().getRegionId();
                if (regionId != null && weatherByRegion.containsKey(regionId)) {
                    List<WeatherData> weatherList = weatherByRegion.get(regionId);
                    for (WeatherData wd : weatherList) {
                        if (wd.getDate() != null && wd.getDate().equals(dateStr)) {
                            setCurrentWeather(rec, wd);
                            break;
                        }
                    }
                }

                if (rec.getTempCurrent() == null) {
                    rec.setTempCurrent("Нет данных");
                    rec.setHumidityCurrent("Нет данных");
                    rec.setPrecipCurrent("Нет данных");
                    rec.setWindCurrent("Нет данных");
                    rec.setWeatherText("Прогноз недоступен");
                }

                result.add(rec);
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        return result;
    }

    private void setRequiredValues(PlantingRecommendation rec, Object crop) {
        Integer minTemp = null, maxTemp = null;
        Integer minHumidity = null, maxHumidity = null;
        Float neededPrecip = null;
        Float maxWind = null;

        if (crop instanceof Crop) {
            Crop c = (Crop) crop;
            if (c.getMinTemp() != null) minTemp = c.getMinTemp().intValue();
            if (c.getMaxTemp() != null) maxTemp = c.getMaxTemp().intValue();
            minHumidity = c.getMinHumidity();
            maxHumidity = c.getMaxHumidity();
            neededPrecip = Float.valueOf(c.getNeededPrecipitation());
            maxWind = Float.valueOf(c.getMaxWind());
        } else if (crop instanceof IndividualUserCrop) {
            IndividualUserCrop ic = (IndividualUserCrop) crop;
            if (ic.getMinTemp() != null) minTemp = ic.getMinTemp().intValue();
            if (ic.getMaxTemp() != null) maxTemp = ic.getMaxTemp().intValue();
            minHumidity = ic.getMinHumidity();
            maxHumidity = ic.getMaxHumidity();
            neededPrecip = Float.valueOf(ic.getNeededPrecipitation());
            maxWind = Float.valueOf(ic.getMaxWind());
        }

        if (minTemp != null && maxTemp != null) {
            rec.setTempRequired(minTemp + "..." + maxTemp + "°C");
        } else if (minTemp != null) {
            rec.setTempRequired("от " + minTemp + "°C");
        } else if (maxTemp != null) {
            rec.setTempRequired("до " + maxTemp + "°C");
        } else {
            rec.setTempRequired("—");
        }

        if (minHumidity != null && maxHumidity != null) {
            rec.setHumidityRequired(minHumidity + "..." + maxHumidity + "%");
        } else if (minHumidity != null) {
            rec.setHumidityRequired("от " + minHumidity + "%");
        } else if (maxHumidity != null) {
            rec.setHumidityRequired("до " + maxHumidity + "%");
        } else {
            rec.setHumidityRequired("—");
        }

        if (neededPrecip != null) {
            rec.setPrecipRequired(String.format(Locale.getDefault(), "%.1f мм", neededPrecip));
        } else {
            rec.setPrecipRequired("—");
        }

        if (maxWind != null) {
            rec.setWindRequired(String.format(Locale.getDefault(), "до %.1f м/с", maxWind));
        } else {
            rec.setWindRequired("—");
        }
    }

    private void setCurrentWeather(PlantingRecommendation rec, WeatherData wd) {
        double tempMin = parseDouble(wd.getTemperatureMin());
        double tempMax = parseDouble(wd.getTemperatureMax());
        double precipitation = parseDouble(wd.getPrecipitation());
        double humidityMin = parseDouble(wd.getHumidityMin());
        double humidityMax = parseDouble(wd.getHumidityMax());
        double windMin = parseDouble(wd.getWindMin());
        double windMax = parseDouble(wd.getWindMax());

        rec.setTempCurrent(String.format(Locale.getDefault(), "%.0f...%.0f°C", tempMin, tempMax));
        rec.setHumidityCurrent(String.format(Locale.getDefault(), "%.0f...%.0f%%", humidityMin, humidityMax));
        rec.setPrecipCurrent(String.format(Locale.getDefault(), "%.1f мм", precipitation));
        rec.setWindCurrent(String.format(Locale.getDefault(), "%.1f...%.1f м/с", windMin, windMax));
        rec.setWeatherText(getWeatherDescription(tempMin, tempMax, precipitation, humidityMin, windMax));
    }

    private String getWeatherDescription(double tempMin, double tempMax, double precipitation, double humidity, double windSpeed) {
        StringBuilder desc = new StringBuilder();

        if (tempMin > 0 && tempMax > 20) {
            desc.append("Тепло");
        } else if (tempMin < 0) {
            desc.append("Холодно");
        } else {
            desc.append("Умеренно");
        }

        if (precipitation > 5) {
            desc.append(", осадки");
        } else if (precipitation > 0) {
            desc.append(", небольшие осадки");
        } else {
            desc.append(", без осадков");
        }

        if (humidity > 70) {
            desc.append(", высокая влажность");
        } else if (humidity < 40) {
            desc.append(", низкая влажность");
        }

        return desc.toString();
    }

    private double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
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