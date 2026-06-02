package com.example.ars;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.Area;
import com.example.ars.models.Category;
import com.example.ars.models.Crop;
import com.example.ars.models.Garden;
import com.example.ars.models.IndividualUserCrop;
import com.example.ars.models.UserCrop;
import com.example.ars.models.UserCategory;
import com.example.ars.models.WeatherData;
import com.example.ars.utils.SharedPreferencesHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddPlantActivity extends AppCompatActivity {

    private ApiService apiService;
    private SharedPreferencesHelper prefsHelper;

    private TextInputLayout tilCategory, tilCrop, tilArea, tilGarden, tilPlantingDate, tilHarvestDate;
    private AutoCompleteTextView actvPlantName;
    private TextInputEditText etPlantingDate, etHarvestDate;
    private TextView tvDescription;

    private List<Category> categories = new ArrayList<>();
    private List<UserCategory> userCategories = new ArrayList<>();
    private List<Area> allAreas = new ArrayList<>();
    private List<Garden> userGardens = new ArrayList<>();
    private Crop selectedCropData = null;

    private Integer selectedUserCategoryId = null;
    private String selectedUserCategoryName = null;
    private Integer selectedGardenId = null;
    private Integer selectedAreaId = null;

    private class PlantListItem {
        Integer id;
        String name;
        String variety;
        String description;
        boolean isIndividual;

        PlantListItem(Integer id, String name, String variety, String description, boolean isIndividual) {
            this.id = id;
            this.name = name;
            this.variety = variety;
            this.description = description;
            this.isIndividual = isIndividual;
        }

        @Override
        public String toString() {
            String displayName = name;
            if (variety != null && !variety.isEmpty()) {
                displayName += " (" + variety + ")";
            }
            if (isIndividual) {
                displayName += " [Моё]";
            }
            return displayName;
        }
    }

    private List<PlantListItem> combinedPlantList = new ArrayList<>();
    private Integer selectedCropId = null;
    private Integer selectedIndividualCropId = null;
    private Integer selectedCategoryId = null;

    private Calendar plantingCalendar = Calendar.getInstance();
    private Calendar harvestCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_plant);

        prefsHelper = new SharedPreferencesHelper(this);
        apiService = RetrofitClient.getApiService();

        initViews();
        loadCategories();
        loadUserCategories();
        loadAllAreas();
        loadUserGardens();
        setupDropdownListeners();
        setupDatePickers();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> showExitDialog());

        MaterialButton btnAddPlant = findViewById(R.id.btnAddPlant);
        btnAddPlant.setOnClickListener(v -> addPlantToUser());

        tilCategory = findViewById(R.id.tilPlantType);
        tilCrop = findViewById(R.id.tilPlantName);
        tilArea = findViewById(R.id.tilArea);
        tilGarden = findViewById(R.id.tilGarden);
        tilPlantingDate = findViewById(R.id.tilPlantingDate);
        tilHarvestDate = findViewById(R.id.tilHarvestDate);
        actvPlantName = findViewById(R.id.actvPlantName);
        tvDescription = findViewById(R.id.tvPlantDescription);
        etPlantingDate = findViewById(R.id.etPlantingDate);
        etHarvestDate = findViewById(R.id.etHarvestDate);
    }

    private void loadUserGardens() {
        apiService.getUserGardens(prefsHelper.getUser().getId()).enqueue(new Callback<List<Garden>>() {
            @Override
            public void onResponse(Call<List<Garden>> call, Response<List<Garden>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userGardens = response.body();
                }
            }
            @Override
            public void onFailure(Call<List<Garden>> call, Throwable t) {}
        });
    }

    private void loadAllAreas() {
        apiService.getUserAreas(prefsHelper.getUser().getId()).enqueue(new Callback<List<Area>>() {
            @Override
            public void onResponse(Call<List<Area>> call, Response<List<Area>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allAreas = response.body();
                    updateAreaDropdown();
                }
            }
            @Override public void onFailure(Call<List<Area>> call, Throwable t) {}
        });
    }

    private void updateAreaDropdown() {
        AutoCompleteTextView actvArea = findViewById(R.id.actvArea);
        ArrayAdapter<Area> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, allAreas);
        actvArea.setAdapter(adapter);
    }

    private void updateGardenDropdown(List<Garden> gardens) {
        AutoCompleteTextView actvGarden = findViewById(R.id.actvGarden);
        if (gardens.isEmpty()) {
            actvGarden.setAdapter(null);
            actvGarden.setText("");
            return;
        }
        ArrayAdapter<Garden> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, gardens);
        actvGarden.setAdapter(adapter);
        actvGarden.setText("");
    }

    private void loadUserCategories() {
        apiService.getUserCategories(prefsHelper.getUser().getId()).enqueue(new Callback<List<UserCategory>>() {
            @Override
            public void onResponse(Call<List<UserCategory>> call, Response<List<UserCategory>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userCategories = response.body();
                    updateCategoryDropdown();
                }
            }
            @Override
            public void onFailure(Call<List<UserCategory>> call, Throwable t) {}
        });
    }

    private void setupDatePickers() {
        DatePickerDialog.OnDateSetListener plantingListener = (view, year, month, dayOfMonth) -> {
            plantingCalendar.set(Calendar.YEAR, year);
            plantingCalendar.set(Calendar.MONTH, month);
            plantingCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            etPlantingDate.setText(sdf.format(plantingCalendar.getTime()));
        };
        DatePickerDialog.OnDateSetListener harvestListener = (view, year, month, dayOfMonth) -> {
            harvestCalendar.set(Calendar.YEAR, year);
            harvestCalendar.set(Calendar.MONTH, month);
            harvestCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            etHarvestDate.setText(sdf.format(harvestCalendar.getTime()));
        };
        etPlantingDate.setOnClickListener(v -> new DatePickerDialog(this, plantingListener,
                plantingCalendar.get(Calendar.YEAR), plantingCalendar.get(Calendar.MONTH), plantingCalendar.get(Calendar.DAY_OF_MONTH)).show());
        etHarvestDate.setOnClickListener(v -> new DatePickerDialog(this, harvestListener,
                harvestCalendar.get(Calendar.YEAR), harvestCalendar.get(Calendar.MONTH), harvestCalendar.get(Calendar.DAY_OF_MONTH)).show());
    }

    private void loadCategories() {
        apiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categories = response.body();
                    updateCategoryDropdown();
                }
            }
            @Override public void onFailure(Call<List<Category>> call, Throwable t) {}
        });
    }

    private void updateCategoryDropdown() {
        AutoCompleteTextView actvPlantType = findViewById(R.id.actvPlantType);
        List<String> categoryNames = new ArrayList<>();
        for (Category c : categories) {
            categoryNames.add(c.getName());
        }
        for (UserCategory uc : userCategories) {
            categoryNames.add(uc.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categoryNames);
        actvPlantType.setAdapter(adapter);
        actvPlantType.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = (String) parent.getItemAtPosition(position);
            UserCategory foundUserCategory = null;
            for (UserCategory uc : userCategories) {
                if (uc.getName().equals(selectedName)) {
                    foundUserCategory = uc;
                    break;
                }
            }
            if (foundUserCategory != null) {
                selectedUserCategoryId = foundUserCategory.getId();
                selectedCategoryId = null;
                selectedUserCategoryName = foundUserCategory.getName();
                tilCategory.setError(null);
                actvPlantName.setText("");
                selectedCropId = null;
                selectedIndividualCropId = null;
                selectedCropData = null;
                tvDescription.setText("Выберите растение...");
                loadCombinedCropsByUserCategory(selectedUserCategoryId);
            } else {
                Category selected = null;
                for (Category c : categories) {
                    if (c.getName().equals(selectedName)) {
                        selected = c;
                        break;
                    }
                }
                if (selected != null) {
                    selectedCategoryId = selected.getId();
                    selectedUserCategoryId = null;
                    tilCategory.setError(null);
                    actvPlantName.setText("");
                    selectedCropId = null;
                    selectedIndividualCropId = null;
                    selectedCropData = null;
                    tvDescription.setText("Выберите растение...");
                    loadCombinedCrops(selected.getName(), selectedCategoryId);
                }
            }
        });
    }

    private void loadCombinedCropsByUserCategory(Integer userCategoryId) {
        combinedPlantList.clear();
        apiService.getIndividualUserCrops(prefsHelper.getUser().getId()).enqueue(new Callback<List<IndividualUserCrop>>() {
            @Override
            public void onResponse(Call<List<IndividualUserCrop>> call, Response<List<IndividualUserCrop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (IndividualUserCrop ic : response.body()) {
                        if (ic.getUserCategoryId() != null && ic.getUserCategoryId().equals(userCategoryId)) {
                            combinedPlantList.add(new PlantListItem(ic.getId(), ic.getName(), ic.getVariety(), ic.getDescription(), true));
                        }
                    }
                }
                updatePlantDropdown();
                if (combinedPlantList.isEmpty()) {
                    tvDescription.setText("Нет растений в этой категории");
                }
            }
            @Override
            public void onFailure(Call<List<IndividualUserCrop>> call, Throwable t) { updatePlantDropdown(); }
        });
    }

    private void setupDropdownListeners() {
        AutoCompleteTextView actvArea = findViewById(R.id.actvArea);
        AutoCompleteTextView actvGarden = findViewById(R.id.actvGarden);
        actvArea.setOnItemClickListener((parent, view, position, id) -> {
            tilArea.setError(null);
            Area selectedArea = (Area) parent.getItemAtPosition(position);
            if (selectedArea != null) {
                selectedAreaId = selectedArea.getId();
                showGardensOnArea(selectedArea);
            }
        });
        actvGarden.setOnItemClickListener((parent, view, position, id) -> {
            Garden selectedGarden = (Garden) parent.getItemAtPosition(position);
            selectedGardenId = selectedGarden.getId();
            tilGarden.setError(null);
        });
        actvPlantName.setOnItemClickListener((parent, view, position, id) -> {
            tilCrop.setError(null);
            PlantListItem selected = (PlantListItem) parent.getItemAtPosition(position);
            if (selected.isIndividual) {
                selectedIndividualCropId = selected.id;
                selectedCropId = null;
                selectedCropData = null;
            } else {
                selectedCropId = selected.id;
                selectedIndividualCropId = null;
                loadCropDetails(selected.id);
            }
            tvDescription.setText(selected.description != null ? selected.description : "Описание отсутствует");
        });
    }

    private void showGardensOnArea(Area selectedArea) {
        List<Garden> gardensOnArea = new ArrayList<>();
        for (Garden garden : userGardens) {
            if (garden.getAreas() != null) {
                for (Area area : garden.getAreas()) {
                    if (area.getId().equals(selectedArea.getId())) {
                        gardensOnArea.add(garden);
                        break;
                    }
                }
            }
        }
        updateGardenDropdown(gardensOnArea);
        selectedGardenId = null;
        if (gardensOnArea.isEmpty()) {
            tilGarden.setError(null);
            Toast.makeText(this, "На этом участке нет огородов", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadCropDetails(Integer cropId) {
        apiService.getCropById(cropId).enqueue(new Callback<Crop>() {
            @Override
            public void onResponse(Call<Crop> call, Response<Crop> response) {
                if (response.isSuccessful() && response.body() != null) {
                    selectedCropData = response.body();
                }
            }
            @Override
            public void onFailure(Call<Crop> call, Throwable t) {}
        });
    }

    private void loadCombinedCrops(String categoryName, Integer categoryId) {
        combinedPlantList.clear();
        apiService.getCropsByCategory(categoryName).enqueue(new Callback<List<Crop>>() {
            @Override
            public void onResponse(Call<List<Crop>> call, Response<List<Crop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Crop c : response.body()) {
                        combinedPlantList.add(new PlantListItem(c.getId(), c.getName(), c.getVariety(), c.getDescription(), false));
                    }
                }
                loadIndividualCrops(categoryId);
            }
            @Override public void onFailure(Call<List<Crop>> call, Throwable t) { loadIndividualCrops(categoryId); }
        });
    }

    private void loadIndividualCrops(Integer categoryId) {
        apiService.getIndividualUserCrops(prefsHelper.getUser().getId()).enqueue(new Callback<List<IndividualUserCrop>>() {
            @Override
            public void onResponse(Call<List<IndividualUserCrop>> call, Response<List<IndividualUserCrop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (IndividualUserCrop ic : response.body()) {
                        if (ic.getCategoryId() != null && ic.getCategoryId().equals(categoryId)) {
                            combinedPlantList.add(new PlantListItem(ic.getId(), ic.getName(), ic.getVariety(), ic.getDescription(), true));
                        }
                    }
                }
                updatePlantDropdown();
            }
            @Override public void onFailure(Call<List<IndividualUserCrop>> call, Throwable t) { updatePlantDropdown(); }
        });
    }

    private void updatePlantDropdown() {
        ArrayAdapter<PlantListItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, combinedPlantList);
        actvPlantName.setAdapter(adapter);
    }

    private boolean validateDates() {
        String plantingDateStr = etPlantingDate.getText().toString().trim();
        String harvestDateStr = etHarvestDate.getText().toString().trim();
        if (plantingDateStr.isEmpty() && harvestDateStr.isEmpty()) {
            return true;
        }
        if (plantingDateStr.isEmpty() && !harvestDateStr.isEmpty()) {
            tilPlantingDate.setError("Укажите дату посадки");
            tilHarvestDate.setError(null);
            return false;
        }
        if (!plantingDateStr.isEmpty() && harvestDateStr.isEmpty()) {
            tilHarvestDate.setError("Укажите дату сбора");
            tilPlantingDate.setError(null);
            return false;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        try {
            Calendar planting = Calendar.getInstance();
            planting.setTime(sdf.parse(plantingDateStr));
            Calendar harvest = Calendar.getInstance();
            harvest.setTime(sdf.parse(harvestDateStr));
            if (harvest.before(planting)) {
                tilHarvestDate.setError("Дата сбора не может быть раньше даты посадки");
                return false;
            }
            if (selectedCropData != null) {
                int month = planting.get(Calendar.MONTH) + 1;
                boolean isWinter = (month == 12 || month == 1 || month == 2);
                if (isWinter && selectedCropData.getCanDirectSow() != null && !selectedCropData.getCanDirectSow()) {
                    tilPlantingDate.setError("Зимой это растение можно сажать только через рассаду");
                    return false;
                }
                if (!validateHarvestDate(selectedCropData, planting, harvest)) {
                    return false;
                }
            }
            tilPlantingDate.setError(null);
            tilHarvestDate.setError(null);
            return true;
        } catch (Exception e) {
            tilPlantingDate.setError("Неверный формат даты");
            return false;
        }
    }

    private boolean validateHarvestDate(Crop crop, Calendar plantingDate, Calendar userHarvestDate) {
        if (crop.getDaysToHarvest() == null || crop.getDaysToHarvest() <= 0) {
            return true;
        }
        Calendar expectedHarvest = (Calendar) plantingDate.clone();
        expectedHarvest.add(Calendar.DAY_OF_YEAR, crop.getDaysToHarvest());
        int toleranceDays = 15;
        Calendar minHarvest = (Calendar) expectedHarvest.clone();
        minHarvest.add(Calendar.DAY_OF_YEAR, -toleranceDays);
        Calendar maxHarvest = (Calendar) expectedHarvest.clone();
        maxHarvest.add(Calendar.DAY_OF_YEAR, toleranceDays);
        if (userHarvestDate.before(minHarvest) || userHarvestDate.after(maxHarvest)) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            tilHarvestDate.setError(String.format("Для '%s' сбор ожидается около %s (±%d дней)", crop.getName(), sdf.format(expectedHarvest.getTime()), toleranceDays));
            return false;
        }
        return true;
    }

    private void addPlantToUser() {
        if (selectedAreaId == null) {
            tilArea.setError("Выберите участок");
            return;
        }
        if (selectedGardenId == null) {
            tilGarden.setError("Выберите огород");
            return;
        }
        if (selectedCropId == null && selectedIndividualCropId == null) {
            tilCrop.setError("Выберите растение");
            return;
        }
        if (selectedCropId != null && selectedCropData == null) {
            Toast.makeText(this, "Загрузка данных о растении, подождите...", Toast.LENGTH_SHORT).show();
            etPlantingDate.postDelayed(this::addPlantToUser, 500);
            return;
        }
        if (!validateDates()) {
            return;
        }
        checkWeatherBeforeAdd();
    }

    private void checkDuplicateAndAdd() {
        apiService.getUserCrops(prefsHelper.getUser().getId()).enqueue(new Callback<List<UserCrop>>() {
            @Override
            public void onResponse(Call<List<UserCrop>> call, Response<List<UserCrop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (UserCrop uc : response.body()) {
                        if (uc.getAreaId() != null && uc.getAreaId().equals(selectedAreaId)) {
                            if (selectedCropId != null && uc.getCropId() != null && uc.getCropId().equals(selectedCropId)) {
                                String plantName = selectedCropData != null ? selectedCropData.getName() : "Это растение";
                                tilCrop.setError(plantName + " уже есть на этом участке!");
                                return;
                            }
                            if (selectedIndividualCropId != null && uc.getIndividualCropId() != null && uc.getIndividualCropId().equals(selectedIndividualCropId)) {
                                tilCrop.setError("Это растение уже есть на этом участке!");
                                return;
                            }
                        }
                    }
                }
                performAddPlant();
            }
            @Override
            public void onFailure(Call<List<UserCrop>> call, Throwable t) {
                performAddPlant();
            }
        });
    }

    private void performAddPlant() {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", prefsHelper.getUser().getId());
        request.put("areaId", selectedAreaId);
        request.put("gardenId", selectedGardenId);
        if (selectedCropId != null) {
            request.put("cropId", selectedCropId);
        } else {
            request.put("individualCropId", selectedIndividualCropId);
        }
        String plantingDateStr = etPlantingDate.getText().toString().trim();
        String harvestDateStr = etHarvestDate.getText().toString().trim();
        if (!plantingDateStr.isEmpty() && !harvestDateStr.isEmpty()) {
            try {
                SimpleDateFormat uiFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                SimpleDateFormat serverFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                java.util.Date plantingDate = uiFormat.parse(plantingDateStr);
                java.util.Date harvestDate = uiFormat.parse(harvestDateStr);
                request.put("plantedAt", serverFormat.format(plantingDate));
                request.put("harvestedAt", serverFormat.format(harvestDate));
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Ошибка формата даты", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        apiService.addUserCrop(request).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddPlantActivity.this, "Растение добавлено!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String errorMsg = "Ошибка сервера: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String error = response.errorBody().string();
                            errorMsg = error;
                        }
                    } catch (Exception e) {}
                    Toast.makeText(AddPlantActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(AddPlantActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Вернуться назад?")
                .setMessage("Все несохраненные данные будут потеряны.")
                .setPositiveButton("Да", (dialog, which) -> finish())
                .setNegativeButton("Нет", null)
                .show();
    }

    private void checkWeatherBeforeAdd() {
        if (selectedAreaId == null) {
            tilArea.setError("Выберите участок");
            return;
        }
        String plantingDateStr = etPlantingDate.getText().toString().trim();
        String harvestDateStr = etHarvestDate.getText().toString().trim();
        if (plantingDateStr.isEmpty() || harvestDateStr.isEmpty()) {
            checkDuplicateAndAdd();
            return;
        }
        Area selectedArea = null;
        for (Area area : allAreas) {
            if (area.getId().equals(selectedAreaId)) {
                selectedArea = area;
                break;
            }
        }
        if (selectedArea == null || selectedArea.getRegionId() == null) {
            checkDuplicateAndAdd();
            return;
        }
        Integer regionId = selectedArea.getRegionId();
        SimpleDateFormat uiFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            java.util.Date plantingDate = uiFormat.parse(plantingDateStr);
            String plantingDateApi = apiFormat.format(plantingDate);
            apiService.getWeatherByDate(regionId, plantingDateApi).enqueue(new Callback<WeatherData>() {
                @Override
                public void onResponse(Call<WeatherData> call, Response<WeatherData> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        WeatherData weather = response.body();
                        checkWeatherConditions(weather, plantingDateStr);
                    } else {
                        checkDuplicateAndAdd();
                    }
                }
                @Override
                public void onFailure(Call<WeatherData> call, Throwable t) {
                    checkDuplicateAndAdd();
                }
            });
        } catch (Exception e) {
            checkDuplicateAndAdd();
        }
    }

    private void checkWeatherConditions(WeatherData weather, String plantingDateStr) {
        boolean hasWarning = false;
        StringBuilder warningMessage = new StringBuilder();
        Crop crop = selectedCropData;
        if (crop == null) {
            checkDuplicateAndAdd();
            return;
        }
        double tempMin = parseDoubleSafe(weather.getTemperatureMin());
        double tempMax = parseDoubleSafe(weather.getTemperatureMax());
        if (crop.getMinTemp() != null && tempMin < crop.getMinTemp()) {
            hasWarning = true;
            warningMessage.append(String.format("Слишком холодно! Минимальная температура %.1f°C ниже нормы (%d°C)\n", tempMin, crop.getMinTemp()));
        }
        if (crop.getMaxTemp() != null && tempMax > crop.getMaxTemp()) {
            hasWarning = true;
            warningMessage.append(String.format("Слишком жарко! Максимальная температура %.1f°C выше нормы (%d°C)\n", tempMax, crop.getMaxTemp()));
        }
        double humidity = parseDoubleSafe(weather.getHumidityMin());
        if (crop.getMinHumidity() != null && humidity < crop.getMinHumidity()) {
            hasWarning = true;
            warningMessage.append(String.format("Низкая влажность! %.0f%% ниже нормы (%d%%)\n", humidity, crop.getMinHumidity()));
        }
        if (crop.getMaxHumidity() != null && humidity > crop.getMaxHumidity()) {
            hasWarning = true;
            warningMessage.append(String.format("Высокая влажность! %.0f%% выше нормы (%d%%)\n", humidity, crop.getMaxHumidity()));
        }
        double wind = parseDoubleSafe(weather.getWindMax());
        if (crop.getMaxWind() != null && wind > crop.getMaxWind()) {
            hasWarning = true;
            warningMessage.append(String.format("Сильный ветер! %.1f м/с выше нормы (%d м/с)\n", wind, crop.getMaxWind()));
        }
        double precipitation = parseDoubleSafe(weather.getPrecipitation());
        if (crop.getNeededPrecipitation() != null && precipitation > crop.getNeededPrecipitation()) {
            hasWarning = true;
            warningMessage.append(String.format("Много осадков! %.1f мм больше нормы (%d мм)\n", precipitation, crop.getNeededPrecipitation()));
        }
        if (hasWarning) {
            new AlertDialog.Builder(this)
                    .setTitle("Неблагоприятная погода")
                    .setMessage("Дата посадки: " + plantingDateStr + "\n\n" + warningMessage.toString() + "\nВы уверены, что хотите посадить растение в таких условиях?")
                    .setPositiveButton("Да, посадить", (dialog, which) -> checkDuplicateAndAdd())
                    .setNegativeButton("Отмена", null)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Погода подходит!")
                    .setMessage("На " + plantingDateStr + " погодные условия благоприятны для посадки " + crop.getName() + ".\n\nДобавить растение?")
                    .setPositiveButton("Да, добавить", (dialog, which) -> checkDuplicateAndAdd())
                    .setNegativeButton("Отмена", null)
                    .show();
        }
    }

    private double parseDoubleSafe(String value) {
        if (value == null || value.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}