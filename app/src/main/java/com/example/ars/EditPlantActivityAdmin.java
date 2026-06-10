package com.example.ars;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
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
import com.example.ars.models.Crop;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditPlantActivityAdmin extends AppCompatActivity {

    private static final String TAG = "EditPlantAdmin";

    private TextInputEditText etPlantName, etVariety, etDescription;
    private TextInputEditText etMinTemp, etMaxTemp;
    private TextInputEditText etMinHumidity, etMaxHumidity;
    private TextInputEditText etPrecipitation, etMaxWind;
    private TextInputEditText etSowingDepth;
    private TextInputEditText etDaysToGermination, etDaysToHarvest;
    private TextInputEditText etWateringInterval, etFertilizingInterval;
    private TextInputEditText etSoilCareInterval, etProtectionInterval;
    private TextInputEditText etPhotoPath;

    private TextInputLayout tilPlantName, tilCategory, tilPhotoPath;
    private AutoCompleteTextView actvCategory;
    private CheckBox cbCanSeedlings, cbCanDirectSow;
    private MaterialButton btnSave, btnDelete;
    private ImageView btnBack, ivSelectedPhoto;
    private View llPhotoPlaceholder, cvPhotoContainer;
    private TextView tvTitle, tvSubtitle;

    private ApiService apiService;
    private List<Category> categories = new ArrayList<>();
    private Integer cropId;
    private String selectedCategoryName = "";
    private Crop currentCrop;
    private String selectedPhotoUri = "";

    private ActivityResultLauncher<Intent> photoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_plant_admin);

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

        etPrecipitation = findViewById(R.id.etNeededPrecipitation);
        etMaxWind = findViewById(R.id.etMaxWind);

        etSowingDepth = findViewById(R.id.etSowingDepth);

        etDaysToGermination = findViewById(R.id.etDaysToGermination);
        etDaysToHarvest = findViewById(R.id.etDaysToHarvest);

        etWateringInterval = findViewById(R.id.etWateringInterval);
        etFertilizingInterval = findViewById(R.id.etFertilizingInterval);
        etSoilCareInterval = findViewById(R.id.etSoilCareInterval);
        etProtectionInterval = findViewById(R.id.etProtectionInterval);

        etPhotoPath = findViewById(R.id.etPhotoPath);
        ivSelectedPhoto = findViewById(R.id.ivSelectedPhoto);
        llPhotoPlaceholder = findViewById(R.id.llPhotoPlaceholder);
        cvPhotoContainer = findViewById(R.id.cvPhotoContainer);

        tilPlantName = findViewById(R.id.tilPlantName);
        tilCategory = findViewById(R.id.tilCategory);
        tilPhotoPath = findViewById(R.id.tilPhotoPath);
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
                        selectedPhotoUri = uri.toString();
                        ivSelectedPhoto.setImageURI(uri);
                        llPhotoPlaceholder.setVisibility(View.GONE);
                        etPhotoPath.setText(selectedPhotoUri);
                    }
                }
        );
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnDelete.setOnClickListener(v -> {
            if (currentCrop != null) {
                showDeleteConfirmation();
            }
        });

        btnSave.setOnClickListener(v -> {
            if (validateAllFields()) {
                updatePlant();
            }
        });

        findViewById(R.id.btnSelectPhoto).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            photoLauncher.launch(intent);
        });

        actvCategory.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategoryName = (String) parent.getItemAtPosition(position);
            tilCategory.setError(null);
        });
    }

    private void loadCropData() {
        apiService.getCropById(cropId).enqueue(new Callback<Crop>() {
            @Override
            public void onResponse(Call<Crop> call, Response<Crop> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentCrop = response.body();
                    fillForm();
                } else {
                    Toast.makeText(EditPlantActivityAdmin.this, "Растение не найдено", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            @Override
            public void onFailure(Call<Crop> call, Throwable t) {
                Toast.makeText(EditPlantActivityAdmin.this, "Ошибка загрузки", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void fillForm() {
        tvSubtitle.setText(currentCrop.getName() + " (ID: " + currentCrop.getId() + ")");

        etPlantName.setText(currentCrop.getName());
        etVariety.setText(currentCrop.getVariety());
        etDescription.setText(currentCrop.getDescription());

        selectedCategoryName = currentCrop.getCategory();
        actvCategory.setText(selectedCategoryName, false);

        setVal(etMinTemp, currentCrop.getMinTemp());
        setVal(etMaxTemp, currentCrop.getMaxTemp());

        setVal(etMinHumidity, currentCrop.getMinHumidity());
        setVal(etMaxHumidity, currentCrop.getMaxHumidity());

        setVal(etPrecipitation, currentCrop.getNeededPrecipitation());
        setVal(etMaxWind, currentCrop.getMaxWind());

        setVal(etSowingDepth, currentCrop.getSowingDepth());

        setVal(etDaysToGermination, currentCrop.getDaysToGermination());
        setVal(etDaysToHarvest, currentCrop.getDaysToHarvest());

        setVal(etWateringInterval, currentCrop.getWateringInterval());
        setVal(etFertilizingInterval, currentCrop.getFertilizingInterval());
        setVal(etSoilCareInterval, currentCrop.getSoilCareInterval());
        setVal(etProtectionInterval, currentCrop.getProtectionInterval());

        if (currentCrop.getCanSeedlings() != null) {
            cbCanSeedlings.setChecked(currentCrop.getCanSeedlings());
        }
        if (currentCrop.getCanDirectSow() != null) {
            cbCanDirectSow.setChecked(currentCrop.getCanDirectSow());
        }

        if (currentCrop.getPhotoPath() != null && !currentCrop.getPhotoPath().isEmpty()) {
            String photoPath = currentCrop.getPhotoPath();
            etPhotoPath.setText(photoPath);
            selectedPhotoUri = photoPath;

            String imageUrl;
            if (photoPath.startsWith("http")) {
                imageUrl = photoPath;
            } else if (photoPath.startsWith("/uploads")) {
                imageUrl = RetrofitClient.BASE_URL + photoPath;
            } else {
                imageUrl = RetrofitClient.BASE_URL + "/uploads/" + photoPath;
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

        if (TextUtils.isEmpty(etPlantName.getText().toString().trim())) {
            tilPlantName.setError("Укажите название");
            isValid = false;
        } else {
            tilPlantName.setError(null);
        }

        if (TextUtils.isEmpty(selectedCategoryName)) {
            tilCategory.setError("Выберите категорию");
            isValid = false;
        } else {
            tilCategory.setError(null);
        }

        Float minTemp = parseFloat(etMinTemp);
        Float maxTemp = parseFloat(etMaxTemp);
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

        Short precip = parseShort(etPrecipitation);
        if (precip != null && (precip < 0 || precip > 500)) {
            etPrecipitation.setError("Осадки должны быть от 0 до 500 мм");
            isValid = false;
        }

        Short maxWind = parseShort(etMaxWind);
        if (maxWind != null && (maxWind < 0 || maxWind > 50)) {
            etMaxWind.setError("Скорость ветра должна быть от 0 до 50 м/с");
            isValid = false;
        }

        Integer sowingDepth = parseInteger(etSowingDepth);
        if (sowingDepth != null && (sowingDepth < 0 || sowingDepth > 50)) {
            etSowingDepth.setError("Глубина посева должна быть от 0 до 50 см");
            isValid = false;
        }

        Integer daysGerm = parseInteger(etDaysToGermination);
        if (daysGerm != null && daysGerm < 0) {
            etDaysToGermination.setError("Не может быть отрицательным");
            isValid = false;
        }
        if (daysGerm != null && daysGerm > 365) {
            etDaysToGermination.setError("Дней до всходов не может быть больше 365");
            isValid = false;
        }

        Integer daysHarvest = parseInteger(etDaysToHarvest);
        if (daysHarvest != null && daysHarvest < 0) {
            etDaysToHarvest.setError("Не может быть отрицательным");
            isValid = false;
        }
        if (daysHarvest != null && daysHarvest > 730) {
            etDaysToHarvest.setError("Дней до урожая не может быть больше 730");
            isValid = false;
        }

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

        if (!cbCanSeedlings.isChecked() && !cbCanDirectSow.isChecked()) {
            Toast.makeText(this, "Выберите хотя бы один способ посадки", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void updatePlant() {
        Crop crop = new Crop();

        crop.setName(etPlantName.getText().toString().trim());
        crop.setVariety(etVariety.getText().toString().trim());
        crop.setCategory(selectedCategoryName);
        crop.setDescription(etDescription.getText().toString().trim());

        crop.setMinTemp(parseShort(etMinTemp));
        crop.setMaxTemp(parseShort(etMaxTemp));

        crop.setMinHumidity(parseInteger(etMinHumidity));
        crop.setMaxHumidity(parseInteger(etMaxHumidity));

        crop.setNeededPrecipitation(parseShort(etPrecipitation));
        crop.setMaxWind(parseShort(etMaxWind));

        crop.setSowingDepth(parseInteger(etSowingDepth));

        crop.setDaysToGermination(parseInteger(etDaysToGermination));
        crop.setDaysToHarvest(parseInteger(etDaysToHarvest));

        crop.setWateringInterval(parseInteger(etWateringInterval));
        crop.setFertilizingInterval(parseInteger(etFertilizingInterval));
        crop.setSoilCareInterval(parseInteger(etSoilCareInterval));
        crop.setProtectionInterval(parseInteger(etProtectionInterval));

        crop.setCanSeedlings(cbCanSeedlings.isChecked());
        crop.setCanDirectSow(cbCanDirectSow.isChecked());

        String photoPath = etPhotoPath.getText().toString().trim();
        if (!TextUtils.isEmpty(selectedPhotoUri) && !TextUtils.isEmpty(photoPath)) {
            crop.setPhotoPath(selectedPhotoUri);
        } else if (!TextUtils.isEmpty(photoPath)) {
            crop.setPhotoPath(photoPath);
        } else {
            crop.setPhotoPath("");
        }

        btnSave.setEnabled(false);

        apiService.updateCrop(cropId, crop).enqueue(new Callback<Crop>() {
            @Override
            public void onResponse(Call<Crop> call, Response<Crop> response) {
                btnSave.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(EditPlantActivityAdmin.this, "Изменения сохранены", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(EditPlantActivityAdmin.this, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Crop> call, Throwable t) {
                btnSave.setEnabled(true);
                Toast.makeText(EditPlantActivityAdmin.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Удаление растения")
                .setMessage("Вы уверены, что хотите удалить \"" + currentCrop.getName() + "\"?")
                .setPositiveButton("Удалить", (d, w) -> {
                    apiService.deleteCrop(cropId).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(EditPlantActivityAdmin.this, "Растение удалено", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            } else {
                                Toast.makeText(EditPlantActivityAdmin.this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(EditPlantActivityAdmin.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void loadCategories() {
        apiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categories = response.body();
                    List<String> names = new ArrayList<>();
                    for (Category c : categories) {
                        names.add(c.getName());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            EditPlantActivityAdmin.this,
                            android.R.layout.simple_dropdown_item_1line,
                            names
                    );
                    actvCategory.setAdapter(adapter);
                }
            }
            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Toast.makeText(EditPlantActivityAdmin.this, "Ошибка загрузки категорий", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setVal(TextInputEditText et, Object val) {
        if (val != null) {
            et.setText(String.valueOf(val));
        } else {
            et.setText("");
        }
    }

    private Float parseFloat(TextInputEditText et) {
        String s = et.getText().toString().trim();
        if (s.isEmpty()) return null;
        try {
            return Float.parseFloat(s.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
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