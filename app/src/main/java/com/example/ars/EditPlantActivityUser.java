package com.example.ars;

import static com.example.ars.api.RetrofitClient.prefsHelper;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditPlantActivityUser extends AppCompatActivity {

    private TextInputEditText etPlantName, etVariety, etDescription;
    private TextInputEditText etMinTemp, etMaxTemp, etMinHumidity, etMaxHumidity;
    private TextInputEditText etNeededPrecipitation, etMaxWind, etSowingDepth;
    private TextInputEditText etDaysToGermination, etDaysToHarvest;
    private TextInputEditText etWateringInterval, etFertilizingInterval, etSoilCareInterval, etProtectionInterval;

    private TextInputLayout tilPlantName, tilCategory;
    private AutoCompleteTextView actvCategory;
    private CheckBox cbCanSeedlings, cbCanDirectSow;
    private MaterialButton btnSave, btnDelete;
    private ImageView btnBack, ivSelectedPhoto;
    private View llPhotoPlaceholder;
    private TextView tvTitle, tvSubtitle;

    private ApiService apiService;
    private Integer cropId;
    private IndividualUserCrop currentCrop;
    private String selectedPhotoPath = "";
    private String currentPhotoPath = "";
    private boolean isPhotoChanged = false;

    private List<Category> systemCategories = new ArrayList<>();
    private List<UserCategory> userCategories = new ArrayList<>();
    private Integer selectedCategoryId = null;
    private Integer selectedUserCategoryId = null;
    private String selectedCategoryName = "";

    private ActivityResultLauncher<Intent> photoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_plant_user);

        cropId = getIntent().getIntExtra("CROP_ID", -1);
        if (cropId == -1) {
            Toast.makeText(this, "Ошибка: ID не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiService = RetrofitClient.getApiService();
        initViews();
        setupPhotoLauncher();
        setupListeners();
        loadCategories();
        loadCropData();
    }

    private void initViews() {
        etPlantName = findViewById(R.id.etPlantName);
        etVariety = findViewById(R.id.etVariety);
        etDescription = findViewById(R.id.etDescription);
        etMinTemp = findViewById(R.id.etMinTemp);
        etMaxTemp = findViewById(R.id.etMaxTemp);
        etMinHumidity = findViewById(R.id.etMinHumidity);
        etMaxHumidity = findViewById(R.id.etMaxHumidity);
        etNeededPrecipitation = findViewById(R.id.etNeededPrecipitation);
        etMaxWind = findViewById(R.id.etMaxWind);
        etSowingDepth = findViewById(R.id.etSowingDepth);
        etDaysToGermination = findViewById(R.id.etDaysToGermination);
        etDaysToHarvest = findViewById(R.id.etDaysToHarvest);
        etWateringInterval = findViewById(R.id.etWateringInterval);
        etFertilizingInterval = findViewById(R.id.etFertilizingInterval);
        etSoilCareInterval = findViewById(R.id.etSoilCareInterval);
        etProtectionInterval = findViewById(R.id.etProtectionInterval);

        ivSelectedPhoto = findViewById(R.id.ivSelectedPhoto);
        llPhotoPlaceholder = findViewById(R.id.llPhotoPlaceholder);
        tilPlantName = findViewById(R.id.tilPlantName);
        tilCategory = findViewById(R.id.tilCategory);
        actvCategory = findViewById(R.id.actvCategory);
        cbCanSeedlings = findViewById(R.id.cbCanSeedlings);
        cbCanDirectSow = findViewById(R.id.cbCanDirectSow);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
    }

    private void setupPhotoLauncher() {
        photoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        selectedPhotoPath = getRealPathFromURI(uri);
                        if (selectedPhotoPath != null) {
                            ivSelectedPhoto.setImageURI(uri);
                            llPhotoPlaceholder.setVisibility(View.GONE);
                            isPhotoChanged = true;
                        }
                    }
                }
        );
    }

    private String getRealPathFromURI(Uri uri) {
        String path = null;
        try {
            String[] projection = {MediaStore.Images.Media.DATA};
            android.database.Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                path = cursor.getString(column_index);
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("EditPlant", "Error getting real path", e);
            path = uri.toString();
        }
        return path;
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnDelete.setOnClickListener(v -> {
            if (currentCrop != null) showDeleteConfirmation();
        });
        btnSave.setOnClickListener(v -> {
            if (validateAllFields()) {
                if (isPhotoChanged && selectedPhotoPath != null) {
                    uploadImageAndUpdate();
                } else {
                    updatePlant();
                }
            }
        });
        findViewById(R.id.btnSelectPhoto).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            photoLauncher.launch(intent);
        });
    }

    private void loadCategories() {
        apiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    systemCategories = response.body();
                    updateCategoryDropdown();
                }
            }
            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Toast.makeText(EditPlantActivityUser.this, "Ошибка загрузки категорий", Toast.LENGTH_SHORT).show();
            }
        });

        if (prefsHelper.getUser() != null) {
            int userId = prefsHelper.getUser().getId();
            apiService.getUserCategories(userId).enqueue(new Callback<List<UserCategory>>() {
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
    }

    private void updateCategoryDropdown() {
        List<String> displayNames = new ArrayList<>();
        for (Category c : systemCategories) {
            displayNames.add(c.getName());
        }
        for (UserCategory uc : userCategories) {
            displayNames.add(uc.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, displayNames);
        actvCategory.setAdapter(adapter);

        actvCategory.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = (String) parent.getItemAtPosition(position);
            selectedCategoryName = selectedName;

            // Определяем, системная это категория или пользовательская
            selectedCategoryId = null;
            selectedUserCategoryId = null;
            for (Category c : systemCategories) {
                if (c.getName().equals(selectedName)) {
                    selectedCategoryId = c.getId().intValue();
                    break;
                }
            }
            for (UserCategory uc : userCategories) {
                if (uc.getName().equals(selectedName)) {
                    selectedUserCategoryId = uc.getId();
                    break;
                }
            }
            tilCategory.setError(null);
        });
    }

    private void uploadImageAndUpdate() {
        btnSave.setEnabled(false);
        btnSave.setText("Загрузка фото...");

        File file = new File(selectedPhotoPath);
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        String categoryName = "user_crops";
        RequestBody categoryReq = RequestBody.create(MediaType.parse("text/plain"), categoryName);

        RetrofitClient.getFileApiService().uploadCropImage(body, categoryReq).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String photoUrl = response.body();
                    currentCrop.setLocalPhotoPath(photoUrl);
                    updatePlant();
                } else {
                    btnSave.setEnabled(true);
                    btnSave.setText("Сохранить");
                    Toast.makeText(EditPlantActivityUser.this, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                btnSave.setEnabled(true);
                btnSave.setText("Сохранить");
                Toast.makeText(EditPlantActivityUser.this, "Ошибка сети при загрузке фото", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCropData() {
        apiService.getUserCropById(cropId).enqueue(new Callback<IndividualUserCrop>() {
            @Override
            public void onResponse(Call<IndividualUserCrop> call, Response<IndividualUserCrop> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentCrop = response.body();
                    fillForm();
                } else {
                    Toast.makeText(EditPlantActivityUser.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            @Override
            public void onFailure(Call<IndividualUserCrop> call, Throwable t) {
                Toast.makeText(EditPlantActivityUser.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void fillForm() {
        tvSubtitle.setText(currentCrop.getName() != null ? currentCrop.getName() : "");

        etPlantName.setText(currentCrop.getName() != null ? currentCrop.getName() : "");
        etVariety.setText(currentCrop.getVariety() != null ? currentCrop.getVariety() : "");
        etDescription.setText(currentCrop.getDescription() != null ? currentCrop.getDescription() : "");

        setVal(etMinTemp, currentCrop.getMinTemp());
        setVal(etMaxTemp, currentCrop.getMaxTemp());
        setVal(etMinHumidity, currentCrop.getMinHumidity());
        setVal(etMaxHumidity, currentCrop.getMaxHumidity());
        setVal(etNeededPrecipitation, currentCrop.getNeededPrecipitation());
        setVal(etMaxWind, currentCrop.getMaxWind());
        setVal(etSowingDepth, currentCrop.getSowingDepth());
        setVal(etDaysToGermination, currentCrop.getDaysToGermination());
        setVal(etDaysToHarvest, currentCrop.getDaysToHarvest());
        setVal(etWateringInterval, currentCrop.getWateringInterval());
        setVal(etFertilizingInterval, currentCrop.getFertilizingInterval());
        setVal(etSoilCareInterval, currentCrop.getSoilCareInterval());
        setVal(etProtectionInterval, currentCrop.getProtectionInterval());

        cbCanSeedlings.setChecked(Boolean.TRUE.equals(currentCrop.getCanSeedlings()));
        cbCanDirectSow.setChecked(Boolean.TRUE.equals(currentCrop.getCanDirectSow()));

        // Заполняем категорию
        if (currentCrop.getCategoryId() != null) {
            for (Category c : systemCategories) {
                if (c.getId().intValue() == currentCrop.getCategoryId()) {
                    actvCategory.setText(c.getName(), false);
                    selectedCategoryName = c.getName();
                    selectedCategoryId = currentCrop.getCategoryId();
                    break;
                }
            }
        } else if (currentCrop.getUserCategoryId() != null) {
            for (UserCategory uc : userCategories) {
                if (uc.getId().equals(currentCrop.getUserCategoryId())) {
                    actvCategory.setText(uc.getName(), false);
                    selectedCategoryName = uc.getName();
                    selectedUserCategoryId = currentCrop.getUserCategoryId();
                    break;
                }
            }
        }

        currentPhotoPath = currentCrop.getLocalPhotoPath();
        if (currentPhotoPath != null && !currentPhotoPath.isEmpty()) {
            String imageUrl;
            if (currentPhotoPath.startsWith("http")) {
                imageUrl = currentPhotoPath;
            } else if (currentPhotoPath.startsWith("/uploads")) {
                imageUrl = RetrofitClient.BASE_URL + currentPhotoPath;
            } else {
                imageUrl = RetrofitClient.BASE_URL + "/uploads/" + currentPhotoPath;
            }

            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_plant)
                    .error(R.drawable.ic_plant)
                    .into(ivSelectedPhoto);
            llPhotoPlaceholder.setVisibility(View.GONE);
        } else {
            ivSelectedPhoto.setImageResource(R.drawable.ic_plant);
            llPhotoPlaceholder.setVisibility(View.VISIBLE);
        }
    }

    private boolean validateAllFields() {
        boolean isValid = true;

        // Название
        if (TextUtils.isEmpty(etPlantName.getText().toString().trim())) {
            tilPlantName.setError("Укажите название");
            isValid = false;
        } else {
            tilPlantName.setError(null);
        }

        // Категория
        if (selectedCategoryId == null && selectedUserCategoryId == null && TextUtils.isEmpty(selectedCategoryName)) {
            tilCategory.setError("Выберите категорию");
            isValid = false;
        } else {
            tilCategory.setError(null);
        }

        // Температура
        Short minTemp = parseShort(etMinTemp);
        Short maxTemp = parseShort(etMaxTemp);
        if (minTemp != null && maxTemp != null && minTemp > maxTemp) {
            etMinTemp.setError("Мин. температура не может быть больше макс.");
            etMaxTemp.setError("Макс. температура не может быть меньше мин.");
            isValid = false;
        }
        if (minTemp != null && (minTemp < -50 || minTemp > 60)) {
            etMinTemp.setError("Температура должна быть от -50 до 60°C");
            isValid = false;
        }
        if (maxTemp != null && (maxTemp < -50 || maxTemp > 60)) {
            etMaxTemp.setError("Температура должна быть от -50 до 60°C");
            isValid = false;
        }

        // Влажность
        Integer minHum = parseInteger(etMinHumidity);
        Integer maxHum = parseInteger(etMaxHumidity);
        if (minHum != null && maxHum != null && minHum > maxHum) {
            etMinHumidity.setError("Мин. влажность не может быть больше макс.");
            etMaxHumidity.setError("Макс. влажность не может быть меньше мин.");
            isValid = false;
        }
        if ((minHum != null && (minHum < 0 || minHum > 100)) ||
                (maxHum != null && (maxHum < 0 || maxHum > 100))) {
            if (minHum != null) etMinHumidity.setError("Влажность должна быть от 0 до 100%");
            if (maxHum != null) etMaxHumidity.setError("Влажность должна быть от 0 до 100%");
            isValid = false;
        }

        // Осадки
        Short precip = parseShort(etNeededPrecipitation);
        if (precip != null && (precip < 0 || precip > 500)) {
            etNeededPrecipitation.setError("Осадки должны быть от 0 до 500 мм");
            isValid = false;
        }

        // Ветер
        Short maxWind = parseShort(etMaxWind);
        if (maxWind != null && (maxWind < 0 || maxWind > 50)) {
            etMaxWind.setError("Скорость ветра должна быть от 0 до 50 м/с");
            isValid = false;
        }

        // Глубина посева
        Integer sowingDepth = parseInteger(etSowingDepth);
        if (sowingDepth != null && (sowingDepth < 0 || sowingDepth > 50)) {
            etSowingDepth.setError("Глубина посева должна быть от 0 до 50 см");
            isValid = false;
        }

        // Дни до всходов
        Integer daysGerm = parseInteger(etDaysToGermination);
        if (daysGerm != null && daysGerm < 0) {
            etDaysToGermination.setError("Не может быть отрицательным");
            isValid = false;
        }
        if (daysGerm != null && daysGerm > 365) {
            etDaysToGermination.setError("Дней до всходов не может быть больше 365");
            isValid = false;
        }

        // Дни до урожая
        Integer daysHarvest = parseInteger(etDaysToHarvest);
        if (daysHarvest != null && daysHarvest < 0) {
            etDaysToHarvest.setError("Не может быть отрицательным");
            isValid = false;
        }
        if (daysHarvest != null && daysHarvest > 730) {
            etDaysToHarvest.setError("Дней до урожая не может быть больше 730");
            isValid = false;
        }

        // Интервалы ухода
        Integer watering = parseInteger(etWateringInterval);
        if (watering != null && watering < 0) {
            etWateringInterval.setError("Не может быть отрицательным");
            isValid = false;
        }
        if (watering != null && watering > 30) {
            etWateringInterval.setError("Интервал полива не более 30 дней");
            isValid = false;
        }

        Integer fertilizing = parseInteger(etFertilizingInterval);
        if (fertilizing != null && fertilizing < 0) {
            etFertilizingInterval.setError("Не может быть отрицательным");
            isValid = false;
        }
        if (fertilizing != null && fertilizing > 90) {
            etFertilizingInterval.setError("Интервал удобрения не более 90 дней");
            isValid = false;
        }

        Integer soilCare = parseInteger(etSoilCareInterval);
        if (soilCare != null && soilCare < 0) {
            etSoilCareInterval.setError("Не может быть отрицательным");
            isValid = false;
        }
        if (soilCare != null && soilCare > 30) {
            etSoilCareInterval.setError("Интервал рыхления не более 30 дней");
            isValid = false;
        }

        Integer protection = parseInteger(etProtectionInterval);
        if (protection != null && protection < 0) {
            etProtectionInterval.setError("Не может быть отрицательным");
            isValid = false;
        }
        if (protection != null && protection > 60) {
            etProtectionInterval.setError("Интервал защиты не более 60 дней");
            isValid = false;
        }

        // Способ посадки
        if (!cbCanSeedlings.isChecked() && !cbCanDirectSow.isChecked()) {
            Toast.makeText(this, "Выберите хотя бы один способ посадки", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void updatePlant() {
        if (currentCrop == null) return;

        btnSave.setEnabled(false);
        btnSave.setText("Сохранение...");

        try {
            currentCrop.setName(etPlantName.getText().toString().trim());
            currentCrop.setVariety(etVariety.getText().toString().trim());
            currentCrop.setDescription(etDescription.getText().toString().trim());

            if (selectedCategoryId != null) {
                currentCrop.setCategoryId(selectedCategoryId);
                currentCrop.setUserCategoryId(null);
            } else if (selectedUserCategoryId != null) {
                currentCrop.setUserCategoryId(selectedUserCategoryId);
                currentCrop.setCategoryId(null);
            }

            currentCrop.setMinTemp(getShortValue(etMinTemp));
            currentCrop.setMaxTemp(getShortValue(etMaxTemp));
            currentCrop.setMinHumidity(getIntegerValue(etMinHumidity));
            currentCrop.setMaxHumidity(getIntegerValue(etMaxHumidity));
            currentCrop.setNeededPrecipitation(getShortValue(etNeededPrecipitation));
            currentCrop.setMaxWind(getShortValue(etMaxWind));
            currentCrop.setSowingDepth(getIntegerValue(etSowingDepth));
            currentCrop.setDaysToGermination(getIntegerValue(etDaysToGermination));
            currentCrop.setDaysToHarvest(getIntegerValue(etDaysToHarvest));
            currentCrop.setWateringInterval(getIntegerValue(etWateringInterval));
            currentCrop.setFertilizingInterval(getIntegerValue(etFertilizingInterval));
            currentCrop.setSoilCareInterval(getIntegerValue(etSoilCareInterval));
            currentCrop.setProtectionInterval(getIntegerValue(etProtectionInterval));
            currentCrop.setCanSeedlings(cbCanSeedlings.isChecked());
            currentCrop.setCanDirectSow(cbCanDirectSow.isChecked());

            apiService.updateUserCrop(cropId, currentCrop).enqueue(new Callback<IndividualUserCrop>() {
                @Override
                public void onResponse(Call<IndividualUserCrop> call, Response<IndividualUserCrop> response) {
                    btnSave.setEnabled(true);
                    btnSave.setText("Сохранить");
                    if (response.isSuccessful()) {
                        Toast.makeText(EditPlantActivityUser.this, "Сохранено", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        String error = "";
                        try {
                            if (response.errorBody() != null) {
                                error = response.errorBody().string();
                            }
                        } catch (Exception e) {}
                        Toast.makeText(EditPlantActivityUser.this, "Ошибка сохранения: " + response.code() + " " + error, Toast.LENGTH_LONG).show();
                    }
                }
                @Override
                public void onFailure(Call<IndividualUserCrop> call, Throwable t) {
                    btnSave.setEnabled(true);
                    btnSave.setText("Сохранить");
                    Toast.makeText(EditPlantActivityUser.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            btnSave.setEnabled(true);
            btnSave.setText("Сохранить");
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("EditPlant", "Error updating plant", e);
        }
    }

    private Short getShortValue(TextInputEditText et) {
        if (et == null) return null;
        String s = et.getText().toString().trim();
        if (s.isEmpty()) return null;
        try {
            return Short.parseShort(s.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer getIntegerValue(TextInputEditText et) {
        if (et == null) return null;
        String s = et.getText().toString().trim();
        if (s.isEmpty()) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Удаление")
                .setMessage("Удалить растение?")
                .setPositiveButton("Да", (d, w) -> deleteCrop())
                .setNegativeButton("Нет", null)
                .show();
    }

    private void deleteCrop() {
        apiService.deleteUserCrop(cropId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> body = response.body();
                    Boolean success = (Boolean) body.get("success");
                    if (success != null && success) {
                        Toast.makeText(EditPlantActivityUser.this, "Растение удалено", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        String error = (String) body.get("error");
                        Toast.makeText(EditPlantActivityUser.this, error != null ? error : "Ошибка удаления", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(EditPlantActivityUser.this, "Ошибка удаления: код " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(EditPlantActivityUser.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setVal(TextInputEditText et, Object val) {
        if (val != null && et != null) {
            et.setText(String.valueOf(val));
        }
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
}