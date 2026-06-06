package com.example.ars;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.adapters.SinglePlantCompatibilityAdapter;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.*;
import com.example.ars.utils.SharedPreferencesHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddPlantActivity extends AppCompatActivity {

    private static final String TAG = "AddPlantActivity";

    private ApiService apiService;
    private SharedPreferencesHelper prefsHelper;

    private TextInputLayout tilCategory, tilCrop, tilArea, tilGarden, tilPlantingDate, tilHarvestDate;
    private AutoCompleteTextView actvPlantName;
    private TextInputEditText etPlantingDate, etHarvestDate;
    private TextView tvDescription, tvCompatibilityLabel;
    private RecyclerView rvCompatibility;
    private MaterialButton btnSaveCompatibility;
    private CardView cvCompatibilityContainer;
    private LinearLayout containerTopNames, containerLeftNames;
    private HorizontalScrollView headerScroll, dataHorizontalScroll;
    private ScrollView sideScroll;

    private List<Category> categories = new ArrayList<>();
    private List<UserCategory> userCategories = new ArrayList<>();
    private List<Area> allAreas = new ArrayList<>();
    private List<Garden> userGardens = new ArrayList<>();
    private List<UserCrop> existingPlantsOnArea = new ArrayList<>();
    private Crop selectedCropData = null;
    private List<IndividualCompatibilityDTO> individualCompatibilities = new ArrayList<>();
    private List<IndividualUserCrop> allUserPlants = new ArrayList<>();
    private List<CompatibilityDTO> currentCompatibilityMatrix = new ArrayList<>();
    private SinglePlantCompatibilityAdapter singlePlantAdapter;

    // Кэш для системной совместимости
    private List<CompatibilityDTO> systemCompatibilityMatrix = new ArrayList<>();
    private boolean systemCompatibilityLoaded = false;

    private Integer selectedUserCategoryId = null;
    private String selectedUserCategoryName = null;
    private Integer selectedGardenId = null;
    private Integer selectedAreaId = null;
    private Garden selectedGarden = null;
    private Area selectedArea = null;
    private boolean isIndividualPlant = false;
    private String newPlantName = "";
    private Integer newPlantId = null;
    private Integer selectedCropId = null;
    private Integer selectedIndividualCropId = null;
    private Integer selectedCategoryId = null;
    private Integer createdUserCropId = null;

    private List<String> userCropNames = new ArrayList<>();
    private Map<Integer, String> plantIdToName = new HashMap<>();
    private List<Integer> orderedPlantIds = new ArrayList<>();

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
        loadIndividualCompatibilities();
        loadSystemCompatibility();
        loadAllUserPlants();
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

        tvCompatibilityLabel = findViewById(R.id.tvCompatibilityLabel);
        rvCompatibility = findViewById(R.id.rvCompatibilityList);
        btnSaveCompatibility = findViewById(R.id.btnSaveCompatibility);
        cvCompatibilityContainer = findViewById(R.id.cvCompatibilityContainer);

        containerTopNames = findViewById(R.id.containerTopNames);
        containerLeftNames = findViewById(R.id.containerLeftNames);
        headerScroll = findViewById(R.id.headerScroll);
        sideScroll = findViewById(R.id.sideScroll);
        dataHorizontalScroll = findViewById(R.id.dataHorizontalScroll);

        btnSaveCompatibility.setOnClickListener(v -> saveCompatibilities());
    }

    private void loadSystemCompatibility() {
        apiService.getCompatibilityMatrix().enqueue(new Callback<List<CompatibilityDTO>>() {
            @Override
            public void onResponse(Call<List<CompatibilityDTO>> call, Response<List<CompatibilityDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    systemCompatibilityMatrix = response.body();
                    systemCompatibilityLoaded = true;
                    Log.d(TAG, "Загружено системных совместимостей: " + systemCompatibilityMatrix.size());

                    // Выводим список несовместимых пар для отладки
                    for (CompatibilityDTO dto : systemCompatibilityMatrix) {
                        if (dto.getStatus() != null && dto.getStatus() == 2) {
                            Log.d(TAG, "НЕСОВМЕСТИМЫ: " + dto.getCropName1() + " + " + dto.getCropName2());
                        }
                    }
                } else {
                    Log.e(TAG, "Ошибка загрузки совместимости: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<List<CompatibilityDTO>> call, Throwable t) {
                Log.e(TAG, "Ошибка: " + t.getMessage());
            }
        });
    }

    private void showCompatibilityBlock() {
        cvCompatibilityContainer.setVisibility(View.VISIBLE);
        tvCompatibilityLabel.setVisibility(View.VISIBLE);
    }

    private void hideCompatibilityBlock() {
        cvCompatibilityContainer.setVisibility(View.GONE);
        tvCompatibilityLabel.setVisibility(View.GONE);
        containerTopNames.removeAllViews();
        containerLeftNames.removeAllViews();
        rvCompatibility.setAdapter(null);
    }

    private void loadAllUserPlants() {
        apiService.getIndividualUserCrops(prefsHelper.getUser().getId())
                .enqueue(new Callback<List<IndividualUserCrop>>() {
                    @Override
                    public void onResponse(Call<List<IndividualUserCrop>> call,
                                           Response<List<IndividualUserCrop>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            allUserPlants = response.body();
                            plantIdToName.clear();
                            for (IndividualUserCrop plant : allUserPlants) {
                                plantIdToName.put(plant.getId(), plant.getName());
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<List<IndividualUserCrop>> call, Throwable t) {}
                });
    }

    private void loadIndividualCompatibilities() {
        apiService.getIndividualCompatibilityMatrix(prefsHelper.getUser().getId())
                .enqueue(new Callback<List<IndividualCompatibilityDTO>>() {
                    @Override
                    public void onResponse(Call<List<IndividualCompatibilityDTO>> call,
                                           Response<List<IndividualCompatibilityDTO>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            individualCompatibilities = response.body();
                        }
                    }
                    @Override
                    public void onFailure(Call<List<IndividualCompatibilityDTO>> call, Throwable t) {}
                });
    }

    private void setupCompatibilityTable() {
        if (allUserPlants.isEmpty()) {
            Toast.makeText(this, "Нет других пользовательских растений", Toast.LENGTH_SHORT).show();
            return;
        }

        userCropNames = new ArrayList<>();
        orderedPlantIds = new ArrayList<>();

        for (IndividualUserCrop plant : allUserPlants) {
            if (!plant.getId().equals(newPlantId)) {
                userCropNames.add(plant.getName());
                orderedPlantIds.add(plant.getId());
            }
        }

        int n = userCropNames.size();
        if (n == 0) {
            Toast.makeText(this, "Нет других растений для сравнения", Toast.LENGTH_SHORT).show();
            return;
        }

        int cellSize = (int) (45 * getResources().getDisplayMetrics().density);

        // Очищаем контейнеры
        containerTopNames.removeAllViews();
        containerLeftNames.removeAllViews();

        // Верхний ряд: имена других растений
        for (String name : userCropNames) {
            TextView tvTop = new TextView(this);
            tvTop.setText(name);
            tvTop.setGravity(Gravity.CENTER);
            tvTop.setTextSize(10);
            tvTop.setSingleLine(true);
            tvTop.setEllipsize(null);
            LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(cellSize, 120);
            tvTop.setLayoutParams(topParams);
            tvTop.setRotation(-90);
            containerTopNames.addView(tvTop);
        }

        // Левый столбец: только НОВОЕ растение (один раз)
        TextView tvLeft = new TextView(this);
        tvLeft.setText(newPlantName);
        tvLeft.setGravity(Gravity.CENTER);
        tvLeft.setTextSize(10);
        tvLeft.setSingleLine(true);
        tvLeft.setEllipsize(null);
        tvLeft.setPadding(8, 0, 8, 0);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(120, cellSize);
        tvLeft.setLayoutParams(leftParams);
        containerLeftNames.addView(tvLeft);

        // Создаем матрицу совместимости для НОВОГО растения со ВСЕМИ другими
        Map<String, Integer> compatibilityMap = new HashMap<>();
        for (IndividualCompatibilityDTO dto : individualCompatibilities) {
            if (dto.getCrop1Id().equals(newPlantId) || dto.getCrop2Id().equals(newPlantId)) {
                Integer otherId = dto.getCrop1Id().equals(newPlantId) ? dto.getCrop2Id() : dto.getCrop1Id();
                String key = newPlantId + "|" + otherId;
                compatibilityMap.put(key, dto.getStatus());
            }
        }

        currentCompatibilityMatrix.clear();
        for (int j = 0; j < orderedPlantIds.size(); j++) {
            Integer otherId = orderedPlantIds.get(j);
            String key = newPlantId + "|" + otherId;
            Integer status = compatibilityMap.getOrDefault(key, 3);

            CompatibilityDTO dto = new CompatibilityDTO(
                    newPlantName,
                    userCropNames.get(j),
                    status
            );
            currentCompatibilityMatrix.add(dto);
        }

        // Используем GridLayoutManager с 1 строкой и N столбцами
        rvCompatibility.setLayoutManager(new GridLayoutManager(this, n));
        singlePlantAdapter = new SinglePlantCompatibilityAdapter(currentCompatibilityMatrix, this::onCellClick);
        rvCompatibility.setAdapter(singlePlantAdapter);

        setupScrollSync();
        showCompatibilityBlock();
    }

    private void onCellClick(CompatibilityDTO item, int position) {
        String plant2Name = userCropNames.get(position);
        Integer otherId = orderedPlantIds.get(position);

        if (newPlantId.equals(otherId)) {
            Toast.makeText(this, "Нельзя менять совместимость с самим собой", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] statusOptions = {"Нет данных", "Конфликт", "Нейтральная", "Хорошая"};
        int currentStatus = item.getStatus() != null ? item.getStatus() : 1;
        int checkedIndex;
        switch (currentStatus) {
            case 2: checkedIndex = 1; break;
            case 3: checkedIndex = 2; break;
            case 4: checkedIndex = 3; break;
            default: checkedIndex = 0; break;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(newPlantName + " + " + plant2Name);

        builder.setSingleChoiceItems(statusOptions, checkedIndex, (dialog, which) -> {
            int newStatus;
            switch (which) {
                case 1: newStatus = 2; break;
                case 2: newStatus = 3; break;
                case 3: newStatus = 4; break;
                default: newStatus = 1; break;
            }

            item.setStatus(newStatus);
            singlePlantAdapter.updateCell(position, newStatus);
            updateCompatibilityInCache(otherId, newStatus);
            dialog.dismiss();
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void updateCompatibilityInCache(Integer otherId, int newStatus) {
        for (IndividualCompatibilityDTO dto : individualCompatibilities) {
            if ((dto.getCrop1Id().equals(newPlantId) && dto.getCrop2Id().equals(otherId)) ||
                    (dto.getCrop1Id().equals(otherId) && dto.getCrop2Id().equals(newPlantId))) {
                dto.setStatus(newStatus);
                return;
            }
        }
        IndividualCompatibilityDTO newDto = new IndividualCompatibilityDTO(newPlantId, otherId, newStatus, prefsHelper.getUser().getId());
        individualCompatibilities.add(newDto);
    }

    private void saveCompatibilities() {
        for (CompatibilityDTO dto : currentCompatibilityMatrix) {
            int index = currentCompatibilityMatrix.indexOf(dto);
            Integer otherId = orderedPlantIds.get(index);
            if (!otherId.equals(newPlantId)) {
                IndividualCompatibilityDTO saveDto = new IndividualCompatibilityDTO(
                        newPlantId, otherId, dto.getStatus(), prefsHelper.getUser().getId()
                );

                apiService.updateIndividualCompatibility(saveDto).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {}
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {}
                });
            }
        }

        Toast.makeText(this, "Совместимость сохранена", Toast.LENGTH_SHORT).show();
        hideCompatibilityBlock();
    }

    private void setupScrollSync() {
        dataHorizontalScroll.setOnScrollChangeListener((v, x, y, oldX, oldY) -> {
            headerScroll.scrollTo(x, 0);
        });

        rvCompatibility.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                sideScroll.scrollBy(0, dy);
            }
        });
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
                hideCompatibilityBlock();
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
                    hideCompatibilityBlock();
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
            selectedArea = (Area) parent.getItemAtPosition(position);
            if (selectedArea != null) {
                selectedAreaId = selectedArea.getId();
                showGardensOnArea(selectedArea);
                loadExistingPlantsOnArea(selectedAreaId, null);
            }
        });

        actvGarden.setOnItemClickListener((parent, view, position, id) -> {
            selectedGarden = (Garden) parent.getItemAtPosition(position);
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
                isIndividualPlant = true;
                newPlantId = selected.id;
                newPlantName = selected.name;
                loadCropDetails(selected.id);
                setupCompatibilityTable();
            } else {
                selectedCropId = selected.id;
                selectedIndividualCropId = null;
                isIndividualPlant = false;
                newPlantId = selected.id;
                newPlantName = selected.name;
                loadCropDetails(selected.id);
                hideCompatibilityBlock();
            }
            tvDescription.setText(selected.description != null ? selected.description : "Описание отсутствует");
        });
    }

    private void loadExistingPlantsOnArea(Integer areaId, Runnable onComplete) {
        apiService.getUserCrops(prefsHelper.getUser().getId()).enqueue(new Callback<List<UserCrop>>() {
            @Override
            public void onResponse(Call<List<UserCrop>> call, Response<List<UserCrop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    existingPlantsOnArea.clear();
                    for (UserCrop uc : response.body()) {
                        if (uc.getAreaId() != null && uc.getAreaId().equals(areaId)) {
                            existingPlantsOnArea.add(uc);
                        }
                    }
                    Log.d(TAG, "Загружено растений на участке: " + existingPlantsOnArea.size());
                }
                if (onComplete != null) {
                    onComplete.run();
                }
            }
            @Override
            public void onFailure(Call<List<UserCrop>> call, Throwable t) {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
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
        selectedGarden = null;
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

        if (plantingDateStr.isEmpty()) {
            tilPlantingDate.setError("Укажите дату посадки");
            return false;
        }
        if (harvestDateStr.isEmpty()) {
            tilHarvestDate.setError("Укажите дату сбора");
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

            tilPlantingDate.setError(null);
            tilHarvestDate.setError(null);
            return true;
        } catch (Exception e) {
            tilPlantingDate.setError("Неверный формат даты");
            return false;
        }
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

        if (selectedGarden != null) {
            for (UserCrop existing : existingPlantsOnArea) {
                if (existing.getGardenId() != null && existing.getGardenId().equals(selectedGardenId)) {
                    tilGarden.setError("На этой грядке уже есть растение!");
                    return;
                }
            }
        }

        if (selectedCropId != null && selectedCropData == null) {
            Toast.makeText(this, "Загрузка данных о растении, подождите...", Toast.LENGTH_SHORT).show();
            etPlantingDate.postDelayed(this::addPlantToUser, 500);
            return;
        }
        if (!validateDates()) {
            return;
        }

        // НОВАЯ ПРОВЕРКА: валидация даты сбора
        if (!validateHarvestDate()) {
            return;
        }

        // Проверяем совместимость перед добавлением
        loadExistingPlantsOnArea(selectedAreaId, () -> {
            checkCompatibilityBeforeAdd();
        });
    }
    private boolean validateHarvestDate() {
        String plantingDateStr = etPlantingDate.getText().toString().trim();
        String harvestDateStr = etHarvestDate.getText().toString().trim();

        // Если даты не указаны, пропускаем проверку
        if (plantingDateStr.isEmpty() || harvestDateStr.isEmpty()) {
            return true;
        }

        // Получаем дни до сбора урожая из выбранной культуры
        Integer daysToHarvest = null;
        if (selectedCropData != null) {
            daysToHarvest = selectedCropData.getDaysToHarvest();
        } else if (selectedIndividualCropId != null) {
            // Если это пользовательское растение, нужно загрузить его данные
            // или использовать already loaded individualCrop data
            for (IndividualUserCrop ic : allUserPlants) {
                if (ic.getId().equals(selectedIndividualCropId)) {
                    daysToHarvest = ic.getDaysToHarvest();
                    break;
                }
            }
        }

        // Если не указано через сколько дней собирать урожай, пропускаем проверку
        if (daysToHarvest == null || daysToHarvest <= 0) {
            return true;
        }

        SimpleDateFormat uiFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        SimpleDateFormat serverFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        try {
            Date plantingDate = uiFormat.parse(plantingDateStr);
            Date harvestDate = uiFormat.parse(harvestDateStr);

            if (plantingDate == null || harvestDate == null) {
                return true;
            }

            // Вычисляем ожидаемую дату сбора
            Calendar expectedHarvest = Calendar.getInstance();
            expectedHarvest.setTime(plantingDate);
            expectedHarvest.add(Calendar.DAY_OF_YEAR, daysToHarvest);

            Calendar harvestCal = Calendar.getInstance();
            harvestCal.setTime(harvestDate);

            // Если дата сбора раньше ожидаемой
            if (harvestCal.before(expectedHarvest)) {
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy", new Locale("ru"));
                String expectedDateStr = outputFormat.format(expectedHarvest.getTime());

                tilHarvestDate.setError("Сбор урожая возможен не ранее " + expectedDateStr +
                        " (через " + daysToHarvest + " дней после посадки)");
                return false;
            }

            tilHarvestDate.setError(null);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    private void checkCompatibilityBeforeAdd() {
        Log.d(TAG, "=== ПРОВЕРКА СОВМЕСТИМОСТИ ===");

        if (!systemCompatibilityLoaded) {
            Log.d(TAG, "Системная совместимость еще не загружена, ждем...");
            Toast.makeText(this, "Загрузка данных о совместимости...", Toast.LENGTH_SHORT).show();
            // Ждем загрузки и пробуем снова
            loadSystemCompatibility();
            etPlantingDate.postDelayed(this::addPlantToUser, 1000);
            return;
        }

        if (existingPlantsOnArea.isEmpty()) {
            Log.d(TAG, "Нет существующих растений - можно сажать");
            performAddPlant();
            return;
        }

        List<String> incompatiblePlants = new ArrayList<>();

        for (UserCrop existing : existingPlantsOnArea) {
            boolean existingIsIndividual = (existing.getIndividualCropId() != null);
            String existingPlantName = getPlantDisplayName(existing);

            int compatibilityStatus = getCompatibilityStatus(existing);

            Log.d(TAG, "Растение: " + existingPlantName + ", статус совместимости: " + compatibilityStatus);

            if (compatibilityStatus == 2) { // 2 = конфликт
                incompatiblePlants.add(existingPlantName);
            }
        }

        if (!incompatiblePlants.isEmpty()) {
            showIncompatibilityDialog(incompatiblePlants);
        } else {
            performAddPlant();
        }
    }

    private int getCompatibilityStatus(UserCrop existing) {
        boolean existingIsIndividual = (existing.getIndividualCropId() != null);
        Integer existingId = existingIsIndividual ? existing.getIndividualCropId() : existing.getCropId();

        // Если оба системные - проверяем системную совместимость
        if (!isIndividualPlant && !existingIsIndividual) {
            return getSystemCompatibilityStatus(newPlantId, existingId);
        }

        // Если оба пользовательские - проверяем пользовательскую совместимость
        if (isIndividualPlant && existingIsIndividual) {
            return getIndividualCompatibilityStatus(newPlantId, existingId);
        }

        // Разные типы - считаем нейтральными
        return 3;
    }

    private int getSystemCompatibilityStatus(Integer cropId1, Integer cropId2) {
        for (CompatibilityDTO dto : systemCompatibilityMatrix) {
            Integer id1 = dto.getCropId1();
            Integer id2 = dto.getCropId2();

            if ((id1 != null && id1.equals(cropId1) && id2 != null && id2.equals(cropId2)) ||
                    (id1 != null && id1.equals(cropId2) && id2 != null && id2.equals(cropId1))) {
                Integer status = dto.getStatus();
                Log.d(TAG, "Найдена совместимость: " + dto.getCropName1() + " + " + dto.getCropName2() + " = " + status);
                return status != null ? status : 3;
            }
        }
        Log.d(TAG, "Совместимость не найдена для " + cropId1 + " и " + cropId2 + ", возвращаем 3");
        return 3;
    }

    private int getIndividualCompatibilityStatus(Integer individualId1, Integer individualId2) {
        for (IndividualCompatibilityDTO dto : individualCompatibilities) {
            if ((dto.getCrop1Id().equals(individualId1) && dto.getCrop2Id().equals(individualId2)) ||
                    (dto.getCrop1Id().equals(individualId2) && dto.getCrop2Id().equals(individualId1))) {
                return dto.getStatus();
            }
        }
        return 3;
    }

    private String getPlantDisplayName(UserCrop plant) {
        if (plant.getCrop() != null) {
            String name = plant.getCrop().getName();
            String variety = plant.getCrop().getVariety();
            if (variety != null && !variety.isEmpty() && !variety.equals("Обычный")) {
                return name + " (" + variety + ")";
            }
            return name;
        } else if (plant.getIndividualCrop() != null) {
            String name = plant.getIndividualCrop().getName();
            String variety = plant.getIndividualCrop().getVariety();
            if (variety != null && !variety.isEmpty() && !variety.equals("Обычный")) {
                return name + " (" + variety + ")";
            }
            return name;
        }
        return "Неизвестное растение";
    }

    private void showIncompatibilityDialog(List<String> incompatiblePlants) {
        StringBuilder message = new StringBuilder();
        message.append("Растение '").append(newPlantName).append("' несовместимо с:\n\n");

        for (String plant : incompatiblePlants) {
            message.append("• ").append(plant).append("\n");
        }

        message.append("\nПосадка может негативно сказаться на росте растений.\n\nПродолжить посадку?");

        new AlertDialog.Builder(this)
                .setTitle("Несовместимые растения")
                .setMessage(message.toString())
                .setPositiveButton("Да, посадить", (dialog, which) -> performAddPlant())
                .setNegativeButton("Отмена", null)
                .show();
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

        SimpleDateFormat uiFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        SimpleDateFormat serverFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        final String plantedAtStr;
        final String harvestedAtStr;

        try {
            String plantingDateStr = etPlantingDate.getText().toString().trim();
            String harvestDateStr = etHarvestDate.getText().toString().trim();
            if (!plantingDateStr.isEmpty() && !harvestDateStr.isEmpty()) {
                Date plantingDate = uiFormat.parse(plantingDateStr);
                Date harvestDate = uiFormat.parse(harvestDateStr);
                plantedAtStr = serverFormat.format(plantingDate);
                harvestedAtStr = serverFormat.format(harvestDate);
                request.put("plantedAt", plantedAtStr);
                request.put("harvestedAt", harvestedAtStr);
            } else {
                plantedAtStr = null;
                harvestedAtStr = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка при обработке дат", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.addUserCrop(request).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> body = response.body();
                    if (body.containsKey("id")) {
                        Number idNum = (Number) body.get("id");
                        createdUserCropId = idNum.intValue();
                    }

                    createPlantingHistoryIfNeeded(plantedAtStr);

                    Toast.makeText(AddPlantActivity.this, "Растение добавлено!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String errorMsg = "Ошибка сервера: " + response.code();
                    Toast.makeText(AddPlantActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(AddPlantActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createPlantingHistoryIfNeeded(String plantedAtStr) {
        if (plantedAtStr == null || plantedAtStr.isEmpty()) {
            return;
        }

        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        if (plantedAtStr.compareTo(todayStr) > 0) {
            return;
        }

        String cropName = "";
        String variety = "";
        String areaName = selectedArea != null ? selectedArea.getName() : "";
        String gardenName = selectedGarden != null ? selectedGarden.getName() : "";

        if (selectedCropData != null) {
            cropName = selectedCropData.getName();
            variety = selectedCropData.getVariety() != null ? selectedCropData.getVariety() : "Обычный";
        } else if (selectedIndividualCropId != null) {
            // Нужно получить данные пользовательского растения
            for (IndividualUserCrop ic : allUserPlants) {
                if (ic.getId().equals(selectedIndividualCropId)) {
                    cropName = ic.getName();
                    variety = ic.getVariety() != null ? ic.getVariety() : "Обычный";
                    break;
                }
            }
        }

        if (cropName.isEmpty()) {
            return;
        }

        Map<String, Object> historyRequest = new HashMap<>();
        historyRequest.put("userId", prefsHelper.getUser().getId());
        historyRequest.put("actionTypeId", 1);
        historyRequest.put("doneAt", plantedAtStr);
        historyRequest.put("cropName", cropName);
        historyRequest.put("variety", variety);
        historyRequest.put("areaName", areaName);
        historyRequest.put("gardenName", gardenName);
        historyRequest.put("regionId", selectedArea != null ? selectedArea.getRegionId() : null);

        String finalCropName = cropName;
        apiService.addGardenHistory(historyRequest).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "История посадки создана для " + finalCropName);
                } else {
                    Log.e(TAG, "Ошибка создания истории посадки: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Ошибка сети при создании истории посадки: " + t.getMessage());
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