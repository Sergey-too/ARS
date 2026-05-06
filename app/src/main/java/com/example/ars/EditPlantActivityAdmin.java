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

    // Поля ввода
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

    // Лаунчер для выбора фото
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
            if (validateFields()) {
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
        tvTitle.setText("Редактирование растения");
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

            if (photoPath.startsWith("http")) {
                Picasso.get().load(photoPath).placeholder(R.drawable.ic_info).into(ivSelectedPhoto);
            } else if (photoPath.startsWith("/")) {
                String url = RetrofitClient.BASE_URL + "/api/img" + photoPath;
                Picasso.get().load(url).placeholder(R.drawable.ic_info).into(ivSelectedPhoto);
            } else {
                Picasso.get().load(Uri.parse(photoPath)).placeholder(R.drawable.ic_info).into(ivSelectedPhoto);
            }
            llPhotoPlaceholder.setVisibility(View.GONE);
        } else {
            llPhotoPlaceholder.setVisibility(View.VISIBLE);
        }
    }

    private boolean validateFields() {
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

        Integer minHum = parseInteger(etMinHumidity);
        Integer maxHum = parseInteger(etMaxHumidity);
        if (minHum != null && maxHum != null && minHum > maxHum) {
            etMinHumidity.setError("Мин. влажность не может быть больше макс.");
            isValid = false;
        }
        if ((minHum != null && (minHum < 0 || minHum > 100)) ||
                (maxHum != null && (maxHum < 0 || maxHum > 100))) {
            if (minHum != null) etMinHumidity.setError("Влажность должна быть от 0 до 100");
            if (maxHum != null) etMaxHumidity.setError("Влажность должна быть от 0 до 100");
            isValid = false;
        }

        Integer daysGerm = parseInteger(etDaysToGermination);
        Integer daysHarvest = parseInteger(etDaysToHarvest);
        if (daysGerm != null && daysGerm < 0) {
            etDaysToGermination.setError("Не может быть отрицательным");
            isValid = false;
        }
        if (daysHarvest != null && daysHarvest < 0) {
            etDaysToHarvest.setError("Не может быть отрицательным");
            isValid = false;
        }

        if (parseInteger(etWateringInterval) != null && parseInteger(etWateringInterval) < 0) {
            etWateringInterval.setError("Не может быть отрицательным");
            isValid = false;
        }
        if (parseInteger(etFertilizingInterval) != null && parseInteger(etFertilizingInterval) < 0) {
            etFertilizingInterval.setError("Не может быть отрицательным");
            isValid = false;
        }
        if (parseInteger(etSoilCareInterval) != null && parseInteger(etSoilCareInterval) < 0) {
            etSoilCareInterval.setError("Не может быть отрицательным");
            isValid = false;
        }
        if (parseInteger(etProtectionInterval) != null && parseInteger(etProtectionInterval) < 0) {
            etProtectionInterval.setError("Не может быть отрицательным");
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

        crop.setMinTemp(parseFloat(etMinTemp));
        crop.setMaxTemp(parseFloat(etMaxTemp));

        crop.setMinHumidity(parseInteger(etMinHumidity));
        crop.setMaxHumidity(parseInteger(etMaxHumidity));

        crop.setNeededPrecipitation(parseFloat(etPrecipitation));
        crop.setMaxWind(parseFloat(etMaxWind));

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
        if (!TextUtils.isEmpty(selectedPhotoUri) && TextUtils.isEmpty(photoPath)) {
            photoPath = selectedPhotoUri;
        }
        crop.setPhotoPath(photoPath);

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
                Toast.makeText(EditPlantActivityAdmin.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
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