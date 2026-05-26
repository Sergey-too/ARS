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
import com.example.ars.models.IndividualUserCrop;
import com.example.ars.models.UserCrop;
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

    private TextInputLayout tilCategory, tilCrop, tilArea, tilPlantingDate, tilHarvestDate;
    private AutoCompleteTextView actvPlantName;
    private TextInputEditText etPlantingDate, etHarvestDate;
    private TextView tvDescription;

    private List<Category> categories = new ArrayList<>();
    private List<Area> userAreas = new ArrayList<>();
    private Crop selectedCropData = null;

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
    private Integer selectedAreaId = null;
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
        loadUserAreas();
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
        tilPlantingDate = findViewById(R.id.tilPlantingDate);
        tilHarvestDate = findViewById(R.id.tilHarvestDate);
        actvPlantName = findViewById(R.id.actvPlantName);
        tvDescription = findViewById(R.id.tvPlantDescription);
        etPlantingDate = findViewById(R.id.etPlantingDate);
        etHarvestDate = findViewById(R.id.etHarvestDate);
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

        etPlantingDate.setOnClickListener(v -> {
            new DatePickerDialog(this, plantingListener,
                    plantingCalendar.get(Calendar.YEAR),
                    plantingCalendar.get(Calendar.MONTH),
                    plantingCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        etHarvestDate.setOnClickListener(v -> {
            new DatePickerDialog(this, harvestListener,
                    harvestCalendar.get(Calendar.YEAR),
                    harvestCalendar.get(Calendar.MONTH),
                    harvestCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void loadUserAreas() {
        apiService.getUserAreas(prefsHelper.getUser().getId()).enqueue(new Callback<List<Area>>() {
            @Override
            public void onResponse(Call<List<Area>> call, Response<List<Area>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userAreas = response.body();
                    updateAreaDropdown();
                }
            }
            @Override public void onFailure(Call<List<Area>> call, Throwable t) {}
        });
    }

    private void updateAreaDropdown() {
        AutoCompleteTextView actvArea = findViewById(R.id.actvArea);
        ArrayAdapter<Area> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, userAreas);
        actvArea.setAdapter(adapter);
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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categoryNames);
        actvPlantType.setAdapter(adapter);
    }

    private void setupDropdownListeners() {
        AutoCompleteTextView actvPlantType = findViewById(R.id.actvPlantType);
        AutoCompleteTextView actvArea = findViewById(R.id.actvArea);

        actvPlantType.setOnItemClickListener((parent, view, position, id) -> {
            Category selected = categories.get(position);
            selectedCategoryId = selected.getId();
            tilCategory.setError(null);

            actvPlantName.setText("");
            selectedCropId = null;
            selectedIndividualCropId = null;
            selectedCropData = null;
            tvDescription.setText("Выберите растение...");

            loadCombinedCrops(selected.getName(), selectedCategoryId);
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

            tvDescription.setText(selected.description != null ?
                    selected.description : "Описание отсутствует");
        });

        actvArea.setOnItemClickListener((parent, view, position, id) -> {
            tilArea.setError(null);
            selectedAreaId = userAreas.get(position).getId();
        });
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
            } else {
                Log.w("AddPlant", "Данные культуры не загружены, проверка сроков сбора пропущена");
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
            tilHarvestDate.setError(String.format(
                    "Для '%s' сбор ожидается около %s (±%d дней)",
                    crop.getName(),
                    sdf.format(expectedHarvest.getTime()),
                    toleranceDays
            ));
            return false;
        }

        return true;
    }

    private void addPlantToUser() {
        if (selectedAreaId == null) {
            tilArea.setError("Выберите участок");
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

        checkDuplicateAndAdd();
    }

    private void checkDuplicateAndAdd() {
        apiService.getUserCrops(prefsHelper.getUser().getId()).enqueue(new Callback<List<UserCrop>>() {
            @Override
            public void onResponse(Call<List<UserCrop>> call, Response<List<UserCrop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (UserCrop uc : response.body()) {
                        if (uc.getAreaId() != null && uc.getAreaId().equals(selectedAreaId)) {
                            if (selectedCropId != null && uc.getCropId() != null &&
                                    uc.getCropId().equals(selectedCropId)) {
                                String plantName = selectedCropData != null ? selectedCropData.getName() : "Это растение";
                                tilCrop.setError(plantName + " уже есть на этом участке!");
                                return;
                            }
                            if (selectedIndividualCropId != null && uc.getIndividualCropId() != null &&
                                    uc.getIndividualCropId().equals(selectedIndividualCropId)) {
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
}