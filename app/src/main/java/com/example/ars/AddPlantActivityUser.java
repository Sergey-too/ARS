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

    private TextInputLayout tilCategory, tilPlantName, tilVariety, tilDescription,
            tilMinTemp, tilMaxTemp, tilMinHumidity, tilMaxHumidity, tilPrecipitation,
            tilMaxWind, tilSowingDepth, tilGermination, tilHarvest,
            tilWatering, tilFertilizing, tilSoilCare, tilProtection;

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
        tilGermination = findViewById(R.id.tilDaysToGermination);
        tilHarvest = findViewById(R.id.tilDaysToHarvest);
        tilWatering = findViewById(R.id.tilWateringInterval);
        tilFertilizing = findViewById(R.id.tilFertilizingInterval);
        tilSoilCare = findViewById(R.id.tilSoilCareInterval);
        tilProtection = findViewById(R.id.tilProtectionInterval);

        cbCanSeedlings = findViewById(R.id.cbCanSeedlings);
        cbCanDirectSow = findViewById(R.id.cbCanDirectSow);

        actvCategory = findViewById(R.id.actvCategory);
        ivSelectedPhoto = findViewById(R.id.ivSelectedPhoto);
        llPhotoPlaceholder = findViewById(R.id.llPhotoPlaceholder);
        btnAddPlant = findViewById(R.id.btnAddPlant);
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

    private void updateCategoryDropdown() {
        List<String> displayNames = new ArrayList<>();

        // Добавляем системные категории
        for (Category c : categories) {
            displayNames.add(c.getName());
        }

        // Добавляем пользовательские категории (без разделителя)
        for (UserCategory uc : userCategories) {
            displayNames.add(uc.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, displayNames);
        actvCategory.setAdapter(adapter);

        actvCategory.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = (String) parent.getItemAtPosition(position);

            // Проверяем, является ли это пользовательской категорией
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
                // Системная категория
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

        if (TextUtils.isEmpty(etName.getText().toString().trim())) {
            tilPlantName.setError("Введите название растения");
            isValid = false;
        }

        if (selectedCategoryId == null && selectedUserCategoryId == null) {
            tilCategory.setError("Выберите категорию");
            isValid = false;
        }

        if (TextUtils.isEmpty(etMinTemp.getText().toString().trim())) {
            tilMinTemp.setError("Укажите мин. температуру");
            isValid = false;
        }

        if (TextUtils.isEmpty(etMaxTemp.getText().toString().trim())) {
            tilMaxTemp.setError("Укажите макс. температуру");
            isValid = false;
        }

        if (TextUtils.isEmpty(etMinHumidity.getText().toString().trim())) {
            tilMinHumidity.setError("Укажите мин. влажность");
            isValid = false;
        }

        if (TextUtils.isEmpty(etMaxHumidity.getText().toString().trim())) {
            tilMaxHumidity.setError("Укажите макс. влажность");
            isValid = false;
        }

        if (TextUtils.isEmpty(etPrecipitation.getText().toString().trim())) {
            tilPrecipitation.setError("Укажите количество осадков");
            isValid = false;
        }

        if (TextUtils.isEmpty(etMaxWind.getText().toString().trim())) {
            tilMaxWind.setError("Укажите макс. скорость ветра");
            isValid = false;
        }

        if (TextUtils.isEmpty(etSowingDepth.getText().toString().trim())) {
            tilSowingDepth.setError("Укажите глубину посева");
            isValid = false;
        }

        if (TextUtils.isEmpty(etGermination.getText().toString().trim())) {
            tilGermination.setError("Укажите дни до всходов");
            isValid = false;
        }

        if (TextUtils.isEmpty(etHarvest.getText().toString().trim())) {
            tilHarvest.setError("Укажите дни до урожая");
            isValid = false;
        }

        if (TextUtils.isEmpty(etWatering.getText().toString().trim())) {
            tilWatering.setError("Укажите интервал полива");
            isValid = false;
        }

        if (TextUtils.isEmpty(etFertilizing.getText().toString().trim())) {
            tilFertilizing.setError("Укажите интервал удобрения");
            isValid = false;
        }

        if (!cbCanSeedlings.isChecked() && !cbCanDirectSow.isChecked()) {
            Toast.makeText(this, "Выберите хотя бы один способ посадки", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (!isValid) return;

        Float minT = parseViewFloat(etMinTemp);
        Float maxT = parseViewFloat(etMaxTemp);
        Integer minH = parseInteger(etMinHumidity);
        Integer maxH = parseInteger(etMaxHumidity);

        if (minT != null && maxT != null && minT > maxT) {
            tilMinTemp.setError("Мин. температура выше максимальной");
            tilMaxTemp.setError("Макс. температура ниже минимальной");
            return;
        }

        if (minT != null && (minT < -50 || minT > 60)) {
            tilMinTemp.setError("Температура должна быть -50...60°C");
            return;
        }

        if (maxT != null && (maxT < -50 || maxT > 60)) {
            tilMaxTemp.setError("Температура должна быть -50...60°C");
            return;
        }

        if (minH != null && (minH < 0 || minH > 100)) {
            tilMinHumidity.setError("Влажность должна быть 0...100%");
            return;
        }

        if (maxH != null && (maxH < 0 || maxH > 100)) {
            tilMaxHumidity.setError("Влажность должна быть 0...100%");
            return;
        }

        if (minH != null && maxH != null && minH > maxH) {
            tilMinHumidity.setError("Мин. влажность выше максимальной");
            tilMaxHumidity.setError("Макс. влажность ниже минимальной");
            return;
        }

        Short precip = parseShort(etPrecipitation);
        if (precip != null && (precip < 0 || precip > 1000)) {
            tilPrecipitation.setError("Осадки должны быть 0...1000 мм");
            return;
        }

        Short maxWind = parseShort(etMaxWind);
        if (maxWind != null && (maxWind < 0 || maxWind > 50)) {
            tilMaxWind.setError("Скорость ветра должна быть 0...50 м/с");
            return;
        }

        Integer sowingDepth = parseInteger(etSowingDepth);
        if (sowingDepth != null && (sowingDepth < 0 || sowingDepth > 50)) {
            tilSowingDepth.setError("Глубина посева должна быть 0...50 см");
            return;
        }

        Integer germination = parseInteger(etGermination);
        if (germination != null && (germination < 0 || germination > 365)) {
            tilGermination.setError("Дни до всходов должны быть 0...365");
            return;
        }

        Integer harvest = parseInteger(etHarvest);
        if (harvest != null && (harvest < 0 || harvest > 730)) {
            tilHarvest.setError("Дни до урожая должны быть 0...730");
            return;
        }

        Integer watering = parseInteger(etWatering);
        if (watering != null && (watering < 0 || watering > 365)) {
            tilWatering.setError("Интервал полива должен быть 0...365");
            return;
        }

        Integer fertilizing = parseInteger(etFertilizing);
        if (fertilizing != null && (fertilizing < 0 || fertilizing > 365)) {
            tilFertilizing.setError("Интервал удобрения должен быть 0...365");
            return;
        }

        if (selectedImagePath != null) {
            uploadImageToServer(selectedImagePath);
        } else {
            savePlant();
        }
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

        // Устанавливаем категорию (системную или пользовательскую)
        if (selectedCategoryId != null) {
            crop.setCategoryId(selectedCategoryId);
        } else if (selectedUserCategoryId != null) {
            crop.setUserCategoryId(selectedUserCategoryId);
        }

        crop.setLocalPhotoPath(serverPhotoUrl);
        crop.setIsCustom(true); // Помечаем как пользовательское растение

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

        if (crop.getMinTemp() != null) etMinTemp.setText(String.valueOf(crop.getMinTemp()));
        if (crop.getMaxTemp() != null) etMaxTemp.setText(String.valueOf(crop.getMaxTemp()));
        if (crop.getMinHumidity() != null) etMinHumidity.setText(String.valueOf(crop.getMinHumidity()));
        if (crop.getMaxHumidity() != null) etMaxHumidity.setText(String.valueOf(crop.getMaxHumidity()));
        if (crop.getNeededPrecipitation() != null) etPrecipitation.setText(String.valueOf(crop.getNeededPrecipitation()));
        if (crop.getMaxWind() != null) etMaxWind.setText(String.valueOf(crop.getMaxWind()));
        if (crop.getSowingDepth() != null) etSowingDepth.setText(String.valueOf(crop.getSowingDepth()));

        if (crop.getDaysToGermination() != null) etGermination.setText(String.valueOf(crop.getDaysToGermination()));
        if (crop.getDaysToHarvest() != null) etHarvest.setText(String.valueOf(crop.getDaysToHarvest()));

        if (crop.getWateringInterval() != null) etWatering.setText(String.valueOf(crop.getWateringInterval()));
        if (crop.getFertilizingInterval() != null) etFertilizing.setText(String.valueOf(crop.getFertilizingInterval()));
        if (crop.getSoilCareInterval() != null) etSoilCare.setText(String.valueOf(crop.getSoilCareInterval()));
        if (crop.getProtectionInterval() != null) etProtection.setText(String.valueOf(crop.getProtectionInterval()));

        cbCanSeedlings.setChecked(crop.getCanSeedlings() != null && crop.getCanSeedlings());
        cbCanDirectSow.setChecked(crop.getCanDirectSow() != null && crop.getCanDirectSow());

        // Определяем тип категории
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
            // Пытаемся загрузить фото по URL
            if (serverPhotoUrl.startsWith("http") || serverPhotoUrl.startsWith("/uploads")) {
                // Для загрузки изображения с сервера нужна дополнительная логика
                // Пока просто показываем плейсхолдер
            }
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
                if (response.isSuccessful() && response.body() != null) fillForm(response.body());
            }

            @Override
            public void onFailure(Call<IndividualUserCrop> call, Throwable t) {
                Toast.makeText(AddPlantActivityUser.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
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