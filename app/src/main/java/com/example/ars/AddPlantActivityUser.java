    package com.example.ars;

    import android.content.Intent;
    import android.database.Cursor;
    import android.net.Uri;
    import android.os.Bundle;
    import android.provider.MediaStore;
    import android.text.TextUtils;
    import android.util.Log;
    import android.view.View;
    import android.widget.ArrayAdapter;
    import android.widget.AutoCompleteTextView;
    import android.widget.CheckBox;
    import android.widget.EditText;
    import android.widget.ImageView;
    import android.widget.Toast;

    import androidx.activity.result.ActivityResultLauncher;
    import androidx.activity.result.contract.ActivityResultContracts;
    import androidx.appcompat.app.AlertDialog;
    import androidx.appcompat.app.AppCompatActivity;

    import com.example.ars.api.ApiService;
    import com.example.ars.api.RetrofitClient;
    import com.example.ars.models.Category;
    import com.example.ars.models.IndividualUserCrop;
    import com.example.ars.models.UserCategory;
    import com.example.ars.utils.SharedPreferencesHelper;
    import com.google.android.material.button.MaterialButton;
    import com.google.android.material.textfield.TextInputEditText;
    import com.google.android.material.textfield.TextInputLayout;

    import java.io.File;
    import java.util.ArrayList;
    import java.util.List;

    import okhttp3.MediaType;
    import okhttp3.MultipartBody;
    import okhttp3.RequestBody;
    import retrofit2.Call;
    import retrofit2.Callback;
    import retrofit2.Response;

    public class AddPlantActivityUser extends AppCompatActivity {

        private ApiService apiService;
        private SharedPreferencesHelper prefsHelper;

        private TextInputEditText etName, etVariety, etDescription, etMinTemp, etMaxTemp, etMinHumidity,
                etMaxHumidity, etPrecipitation, etMaxWind, etSowingDepth, etGermination, etHarvest,
                etWatering, etFertilizing, etSoilCare, etProtection;

        private TextInputLayout tilPlantName, tilVariety, tilDescription, tilCategory,
                tilMinTemp, tilMaxTemp, tilMinHumidity, tilMaxHumidity, tilPrecipitation,
                tilMaxWind, tilSowingDepth, tilDaysToGermination, tilDaysToHarvest,
                tilWateringInterval, tilFertilizingInterval, tilSoilCareInterval, tilProtectionInterval;

        private CheckBox cbCanSeedlings, cbCanDirectSow;
        private AutoCompleteTextView actvCategory;
        private ImageView ivSelectedPhoto;
        private View llPhotoPlaceholder;
        private MaterialButton btnAddPlant;

        private List<Category> categories = new ArrayList<>();
        private List<UserCategory> userCategories = new ArrayList<>();
        private Integer selectedCategoryId = null;
        private Integer selectedUserCategoryId = null;
        private String selectedCategoryName = "";
        private String selectedImagePath = null;
        private String serverPhotoUrl = "";
        private Integer editingCropId = null;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_add_plant_user);

            apiService = RetrofitClient.getApiService();
            prefsHelper = new SharedPreferencesHelper(this);

            initViews();
            setupListeners();
            loadCategories();
            loadUserCategories();

            if (getIntent().hasExtra("CROP_ID")) {
                editingCropId = getIntent().getIntExtra("CROP_ID", -1);
                loadCropData(editingCropId);
                btnAddPlant.setText("Сохранить изменения");
            }
        }

        private void initViews() {
            etName = findViewById(R.id.etPlantName);
            etVariety = findViewById(R.id.etVariety);
            etDescription = findViewById(R.id.etDescription);
            etMinTemp = findViewById(R.id.etMinTemp);
            etMaxTemp = findViewById(R.id.etMaxTemp);
            etMinHumidity = findViewById(R.id.etMinHumidity);
            etMaxHumidity = findViewById(R.id.etMaxHumidity);
            etPrecipitation = findViewById(R.id.etNeededPrecipitation);
            etMaxWind = findViewById(R.id.etMaxWind);
            etSowingDepth = findViewById(R.id.etSowingDepth);
            etGermination = findViewById(R.id.etDaysToGermination);
            etHarvest = findViewById(R.id.etDaysToHarvest);
            etWatering = findViewById(R.id.etWateringInterval);
            etFertilizing = findViewById(R.id.etFertilizingInterval);
            etSoilCare = findViewById(R.id.etSoilCareInterval);
            etProtection = findViewById(R.id.etProtectionInterval);

            tilPlantName = findViewById(R.id.tilPlantName);
            tilVariety = findViewById(R.id.tilVariety);
            tilDescription = findViewById(R.id.tilDescription);
            tilCategory = findViewById(R.id.tilCategory);
            tilMinTemp = findViewById(R.id.tilMinTemp);
            tilMaxTemp = findViewById(R.id.tilMaxTemp);
            tilMinHumidity = findViewById(R.id.tilMinHumidity);
            tilMaxHumidity = findViewById(R.id.tilMaxHumidity);
            tilPrecipitation = findViewById(R.id.tilPrecipitation);
            tilMaxWind = findViewById(R.id.tilMaxWind);
            tilSowingDepth = findViewById(R.id.tilSowingDepth);
            tilDaysToGermination = findViewById(R.id.tilDaysToGermination);
            tilDaysToHarvest = findViewById(R.id.tilDaysToHarvest);
            tilWateringInterval = findViewById(R.id.tilWateringInterval);
            tilFertilizingInterval = findViewById(R.id.tilFertilizingInterval);
            tilSoilCareInterval = findViewById(R.id.tilSoilCareInterval);
            tilProtectionInterval = findViewById(R.id.tilProtectionInterval);

            cbCanSeedlings = findViewById(R.id.cbCanSeedlings);
            cbCanDirectSow = findViewById(R.id.cbCanDirectSow);

            actvCategory = findViewById(R.id.actvCategory);
            ivSelectedPhoto = findViewById(R.id.ivSelectedPhoto);
            llPhotoPlaceholder = findViewById(R.id.llPhotoPlaceholder);
            btnAddPlant = findViewById(R.id.btnAddPlant);
        }

        private void setupListeners() {
            findViewById(R.id.btnBack).setOnClickListener(v -> showExitDialog());

            ActivityResultLauncher<Intent> photoLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null) {
                                ivSelectedPhoto.setImageURI(uri);
                                llPhotoPlaceholder.setVisibility(View.GONE);
                                selectedImagePath = getRealPathFromURI(uri);
                                Log.d("PHOTO_DEBUG", "Выбран путь: " + selectedImagePath);
                            }
                        }
                    }
            );

            findViewById(R.id.btnSelectPhoto).setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                photoLauncher.launch(intent);
            });

            btnAddPlant.setOnClickListener(v -> validateAndSave());
        }

        private void showAddCategoryDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = getLayoutInflater().inflate(R.layout.dialog_add_category, null);
            EditText input = view.findViewById(R.id.etInputName);

            builder.setTitle("Новая категория");
            builder.setView(view);
            builder.setPositiveButton("Создать", (dialog, which) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) {
                    createUserCategory(name);
                } else {
                    Toast.makeText(this, "Введите название категории", Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Отмена", null);
            builder.show();
        }

        private void createUserCategory(String name) {
            UserCategory category = new UserCategory();
            category.setUserId(prefsHelper.getUser().getId());
            category.setName(name);

            apiService.createUserCategory(category).enqueue(new Callback<UserCategory>() {
                @Override
                public void onResponse(Call<UserCategory> call, Response<UserCategory> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AddPlantActivityUser.this, "Категория создана", Toast.LENGTH_SHORT).show();
                        loadUserCategories();
                    } else {
                        Toast.makeText(AddPlantActivityUser.this, "Ошибка: возможно, такая категория уже есть", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<UserCategory> call, Throwable t) {
                    Toast.makeText(AddPlantActivityUser.this, "Ошибка: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
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
                public void onFailure(Call<List<UserCategory>> call, Throwable t) {
                }
            });
        }

        private void updateCategoryDropdown() {
            List<String> displayNames = new ArrayList<>();

            for (Category c : categories) {
                displayNames.add(c.getName());
            }

            for (UserCategory uc : userCategories) {
                displayNames.add(uc.getName());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, displayNames);
            actvCategory.setAdapter(adapter);

            actvCategory.setOnItemClickListener((parent, view, position, id) -> {
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
                    selectedCategoryName = foundUserCategory.getName();
                } else {
                    Category selected = null;
                    for (Category c : categories) {
                        if (c.getName().equals(selectedName)) {
                            selected = c;
                            break;
                        }
                    }
                    if (selected != null) {
                        selectedCategoryId = selected.getId().intValue();
                        selectedUserCategoryId = null;
                        selectedCategoryName = selected.getName();
                    }
                }
                tilCategory.setError(null);
            });
        }

        private void validateAndSave() {
            boolean isValid = true;

            clearAllErrors();

            if (TextUtils.isEmpty(etName.getText().toString().trim())) {
                tilPlantName.setError("Введите название растения");
                isValid = false;
            }

            if (TextUtils.isEmpty(etVariety.getText().toString().trim())) {
                tilVariety.setError("Введите сорт растения");
                isValid = false;
            }

            if (selectedCategoryId == null && selectedUserCategoryId == null) {
                tilCategory.setError("Выберите категорию");
                isValid = false;
            }

            Float minT = null;
            if (TextUtils.isEmpty(etMinTemp.getText().toString().trim())) {
                tilMinTemp.setError("Укажите минимальную температуру");
                isValid = false;
            } else {
                minT = parseViewFloat(etMinTemp);
                if (minT != null && (minT < -50 || minT > 60)) {
                    tilMinTemp.setError("Температура должна быть от -50 до 60°C");
                    isValid = false;
                }
            }

            Float maxT = null;
            if (TextUtils.isEmpty(etMaxTemp.getText().toString().trim())) {
                tilMaxTemp.setError("Укажите максимальную температуру");
                isValid = false;
            } else {
                maxT = parseViewFloat(etMaxTemp);
                if (maxT != null && (maxT < -50 || maxT > 60)) {
                    tilMaxTemp.setError("Температура должна быть от -50 до 60°C");
                    isValid = false;
                }
            }

            if (minT != null && maxT != null && minT > maxT) {
                tilMinTemp.setError("Мин. температура не может быть выше максимальной");
                tilMaxTemp.setError("Макс. температура не может быть ниже минимальной");
                isValid = false;
            }

            Integer minH = null;
            if (TextUtils.isEmpty(etMinHumidity.getText().toString().trim())) {
                tilMinHumidity.setError("Укажите минимальную влажность");
                isValid = false;
            } else {
                minH = parseInteger(etMinHumidity);
                if (minH != null && (minH < 0 || minH > 100)) {
                    tilMinHumidity.setError("Влажность должна быть от 0 до 100%");
                    isValid = false;
                }
            }

            Integer maxH = null;
            if (TextUtils.isEmpty(etMaxHumidity.getText().toString().trim())) {
                tilMaxHumidity.setError("Укажите максимальную влажность");
                isValid = false;
            } else {
                maxH = parseInteger(etMaxHumidity);
                if (maxH != null && (maxH < 0 || maxH > 100)) {
                    tilMaxHumidity.setError("Влажность должна быть от 0 до 100%");
                    isValid = false;
                }
            }

            if (minH != null && maxH != null && minH > maxH) {
                tilMinHumidity.setError("Мин. влажность не может быть выше максимальной");
                tilMaxHumidity.setError("Макс. влажность не может быть ниже минимальной");
                isValid = false;
            }

            if (TextUtils.isEmpty(etPrecipitation.getText().toString().trim())) {
                tilPrecipitation.setError("Укажите количество осадков");
                isValid = false;
            } else {
                Short precip = parseShort(etPrecipitation);
                if (precip != null && (precip < 0 || precip > 1000)) {
                    tilPrecipitation.setError("Осадки должны быть от 0 до 1000 мм");
                    isValid = false;
                }
            }

            if (TextUtils.isEmpty(etMaxWind.getText().toString().trim())) {
                tilMaxWind.setError("Укажите максимальную скорость ветра");
                isValid = false;
            } else {
                Short maxWind = parseShort(etMaxWind);
                if (maxWind != null && (maxWind < 0 || maxWind > 50)) {
                    tilMaxWind.setError("Скорость ветра должна быть от 0 до 50 м/с");
                    isValid = false;
                }
            }

            if (TextUtils.isEmpty(etSowingDepth.getText().toString().trim())) {
                tilSowingDepth.setError("Укажите глубину посева");
                isValid = false;
            } else {
                Integer sowingDepth = parseInteger(etSowingDepth);
                if (sowingDepth != null && (sowingDepth < 0 || sowingDepth > 50)) {
                    tilSowingDepth.setError("Глубина посева должна быть от 0 до 50 см");
                    isValid = false;
                }
            }

            if (TextUtils.isEmpty(etGermination.getText().toString().trim())) {
                tilDaysToGermination.setError("Укажите дни до всходов");
                isValid = false;
            } else {
                Integer germination = parseInteger(etGermination);
                if (germination != null && (germination < 0 || germination > 365)) {
                    tilDaysToGermination.setError("Дни до всходов должны быть от 0 до 365");
                    isValid = false;
                }
            }

            if (TextUtils.isEmpty(etHarvest.getText().toString().trim())) {
                tilDaysToHarvest.setError("Укажите дни до урожая");
                isValid = false;
            } else {
                Integer harvest = parseInteger(etHarvest);
                if (harvest != null && (harvest < 0 || harvest > 730)) {
                    tilDaysToHarvest.setError("Дни до урожая должны быть от 0 до 730");
                    isValid = false;
                }
            }

            if (TextUtils.isEmpty(etWatering.getText().toString().trim())) {
                tilWateringInterval.setError("Укажите интервал полива");
                isValid = false;
            } else {
                Integer watering = parseInteger(etWatering);
                if (watering != null && (watering < 0 || watering > 365)) {
                    tilWateringInterval.setError("Интервал полива должен быть от 0 до 365 дней");
                    isValid = false;
                }
            }

            if (TextUtils.isEmpty(etFertilizing.getText().toString().trim())) {
                tilFertilizingInterval.setError("Укажите интервал удобрения");
                isValid = false;
            } else {
                Integer fertilizing = parseInteger(etFertilizing);
                if (fertilizing != null && (fertilizing < 0 || fertilizing > 365)) {
                    tilFertilizingInterval.setError("Интервал удобрения должен быть от 0 до 365 дней");
                    isValid = false;
                }
            }

            if (TextUtils.isEmpty(etSoilCare.getText().toString().trim())) {
                tilSoilCareInterval.setError("Укажите интервал рыхления");
                isValid = false;
            } else {
                Integer soilCare = parseInteger(etSoilCare);
                if (soilCare != null && (soilCare < 0 || soilCare > 365)) {
                    tilSoilCareInterval.setError("Интервал рыхления должен быть от 0 до 365 дней");
                    isValid = false;
                }
            }

            if (TextUtils.isEmpty(etProtection.getText().toString().trim())) {
                tilProtectionInterval.setError("Укажите интервал защиты");
                isValid = false;
            } else {
                Integer protection = parseInteger(etProtection);
                if (protection != null && (protection < 0 || protection > 365)) {
                    tilProtectionInterval.setError("Интервал защиты должен быть от 0 до 365 дней");
                    isValid = false;
                }
            }

            if (!cbCanSeedlings.isChecked() && !cbCanDirectSow.isChecked()) {
                Toast.makeText(this, "Выберите хотя бы один способ посадки", Toast.LENGTH_SHORT).show();
                isValid = false;
            }

            if (!isValid) return;

            if (selectedImagePath != null) {
                uploadImageToServer(selectedImagePath);
            } else {
                savePlant();
            }
        }

        private void clearAllErrors() {
            if (tilPlantName != null) tilPlantName.setError(null);
            if (tilVariety != null) tilVariety.setError(null);
            if (tilDescription != null) tilDescription.setError(null);
            if (tilCategory != null) tilCategory.setError(null);
            if (tilMinTemp != null) tilMinTemp.setError(null);
            if (tilMaxTemp != null) tilMaxTemp.setError(null);
            if (tilMinHumidity != null) tilMinHumidity.setError(null);
            if (tilMaxHumidity != null) tilMaxHumidity.setError(null);
            if (tilPrecipitation != null) tilPrecipitation.setError(null);
            if (tilMaxWind != null) tilMaxWind.setError(null);
            if (tilSowingDepth != null) tilSowingDepth.setError(null);
            if (tilDaysToGermination != null) tilDaysToGermination.setError(null);
            if (tilDaysToHarvest != null) tilDaysToHarvest.setError(null);
            if (tilWateringInterval != null) tilWateringInterval.setError(null);
            if (tilFertilizingInterval != null) tilFertilizingInterval.setError(null);
            if (tilSoilCareInterval != null) tilSoilCareInterval.setError(null);
            if (tilProtectionInterval != null) tilProtectionInterval.setError(null);
        }

        private void uploadImageToServer(String localPath) {
            btnAddPlant.setEnabled(false);
            btnAddPlant.setText("ОБРАБОТКА ФОТО...");

            File file = new File(localPath);
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            String folderName = selectedCategoryName.toLowerCase().replaceAll("\\s+", "_");
            RequestBody categoryReq = RequestBody.create(MediaType.parse("text/plain"), folderName);

            RetrofitClient.getFileApiService().uploadCropImage(body, categoryReq).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        serverPhotoUrl = response.body();
                        savePlant();
                    } else {
                        resetButton();
                        Toast.makeText(AddPlantActivityUser.this, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    resetButton();
                    Log.e("UPLOAD_FAIL", "Сеть упала при загрузке фото: " + t.getMessage());
                    Toast.makeText(AddPlantActivityUser.this, "Ошибка сети при загрузке фото", Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void savePlant() {
            btnAddPlant.setEnabled(false);
            btnAddPlant.setText("СОХРАНЕНИЕ...");

            IndividualUserCrop crop = new IndividualUserCrop();
            crop.setUserId(prefsHelper.getUser().getId());

            if (selectedCategoryId != null) {
                crop.setCategoryId(selectedCategoryId);
            } else if (selectedUserCategoryId != null) {
                crop.setUserCategoryId(selectedUserCategoryId);
            }

            crop.setLocalPhotoPath(serverPhotoUrl);
            crop.setIsCustom(true);

            crop.setName(etName.getText().toString().trim());
            crop.setVariety(etVariety.getText().toString().trim());
            crop.setDescription(etDescription.getText().toString().trim());

            crop.setMinTemp(parseShort(etMinTemp));
            crop.setMaxTemp(parseShort(etMaxTemp));
            crop.setMaxWind(parseShort(etMaxWind));
            crop.setNeededPrecipitation(parseShort(etPrecipitation));
            crop.setSowingDepth(parseInteger(etSowingDepth));

            crop.setMinHumidity(parseInteger(etMinHumidity));
            crop.setMaxHumidity(parseInteger(etMaxHumidity));

            crop.setDaysToGermination(parseInteger(etGermination));
            crop.setDaysToHarvest(parseInteger(etHarvest));

            crop.setWateringInterval(parseInteger(etWatering));
            crop.setFertilizingInterval(parseInteger(etFertilizing));
            crop.setSoilCareInterval(parseInteger(etSoilCare));
            crop.setProtectionInterval(parseInteger(etProtection));

            crop.setCanSeedlings(cbCanSeedlings.isChecked());
            crop.setCanDirectSow(cbCanDirectSow.isChecked());

            Callback<IndividualUserCrop> callback = new Callback<IndividualUserCrop>() {
                @Override
                public void onResponse(Call<IndividualUserCrop> call, Response<IndividualUserCrop> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AddPlantActivityUser.this,
                                editingCropId == null ? "Растение добавлено" : "Изменения сохранены",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        resetButton();
                        Log.e("DB_SAVE_ERROR", "Код: " + response.code());
                        Toast.makeText(AddPlantActivityUser.this, "Ошибка сохранения: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<IndividualUserCrop> call, Throwable t) {
                    resetButton();
                    Toast.makeText(AddPlantActivityUser.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            };

            if (editingCropId == null) {
                apiService.createUserCrop(crop).enqueue(callback);
            } else {
                apiService.updateUserCrop(editingCropId, crop).enqueue(callback);
            }
        }

        private void fillForm(IndividualUserCrop crop) {
            etName.setText(crop.getName());
            etVariety.setText(crop.getVariety());
            etDescription.setText(crop.getDescription());

            setValue(etMinTemp, crop.getMinTemp());
            setValue(etMaxTemp, crop.getMaxTemp());
            setValue(etMinHumidity, crop.getMinHumidity());
            setValue(etMaxHumidity, crop.getMaxHumidity());
            setValue(etPrecipitation, crop.getNeededPrecipitation());
            setValue(etMaxWind, crop.getMaxWind());
            setValue(etSowingDepth, crop.getSowingDepth());
            setValue(etGermination, crop.getDaysToGermination());
            setValue(etHarvest, crop.getDaysToHarvest());
            setValue(etWatering, crop.getWateringInterval());
            setValue(etFertilizing, crop.getFertilizingInterval());
            setValue(etSoilCare, crop.getSoilCareInterval());
            setValue(etProtection, crop.getProtectionInterval());

            cbCanSeedlings.setChecked(crop.getCanSeedlings() != null && crop.getCanSeedlings());
            cbCanDirectSow.setChecked(crop.getCanDirectSow() != null && crop.getCanDirectSow());

            if (crop.getUserCategoryId() != null) {
                selectedUserCategoryId = crop.getUserCategoryId();
                selectedCategoryId = null;
                for (UserCategory uc : userCategories) {
                    if (uc.getId().equals(selectedUserCategoryId)) {
                        actvCategory.setText(uc.getName(), false);
                        selectedCategoryName = uc.getName();
                        break;
                    }
                }
            } else if (crop.getCategoryId() != null) {
                selectedCategoryId = crop.getCategoryId();
                selectedUserCategoryId = null;
                for (Category c : categories) {
                    if (c.getId().equals(Long.valueOf(selectedCategoryId))) {
                        actvCategory.setText(c.getName(), false);
                        selectedCategoryName = c.getName();
                        break;
                    }
                }
            }

            if (crop.getLocalPhotoPath() != null && !crop.getLocalPhotoPath().isEmpty()) {
                serverPhotoUrl = crop.getLocalPhotoPath();
                llPhotoPlaceholder.setVisibility(View.GONE);
            }
        }

        private void setValue(TextInputEditText et, Object val) {
            if (val != null) {
                et.setText(String.valueOf(val));
            }
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

                @Override
                public void onFailure(Call<List<Category>> call, Throwable t) {
                    Toast.makeText(AddPlantActivityUser.this, "Ошибка загрузки категорий", Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void loadCropData(Integer id) {
            apiService.getUserCropById(id).enqueue(new Callback<IndividualUserCrop>() {
                @Override
                public void onResponse(Call<IndividualUserCrop> call, Response<IndividualUserCrop> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        fillForm(response.body());
                    }
                }

                @Override
                public void onFailure(Call<IndividualUserCrop> call, Throwable t) {
                    Toast.makeText(AddPlantActivityUser.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                }
            });
        }

        private String getRealPathFromURI(Uri contentUri) {
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
            if (cursor == null) return null;
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        }

        private void resetButton() {
            btnAddPlant.setEnabled(true);
            btnAddPlant.setText(editingCropId == null ? "Добавить растение" : "Сохранить изменения");
        }

        private Short parseShort(TextInputEditText et) {
            String s = et.getText().toString().trim();
            if (s.isEmpty()) return null;
            try {
                return Short.parseShort(s.replace(",", "."));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private Integer parseInteger(TextInputEditText et) {
            String s = et.getText().toString().trim();
            if (s.isEmpty()) return null;
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private Float parseViewFloat(EditText editText) {
            String text = editText.getText().toString().trim();
            if (text.isEmpty()) return null;
            try {
                return Float.parseFloat(text.replace(",", "."));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private void showExitDialog() {
            new AlertDialog.Builder(this)
                    .setTitle("Выйти?")
                    .setMessage("Данные не будут сохранены")
                    .setPositiveButton("Да", (d, w) -> finish())
                    .setNegativeButton("Нет", null)
                    .show();
        }
    }