package com.example.ars;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
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
import com.example.ars.models.IndividualUserCrop;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.squareup.picasso.Picasso;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditPlantActivityUser extends AppCompatActivity {

    private TextInputEditText etPlantName, etVariety, etDescription;
    private TextInputEditText etMinTemp, etMaxTemp, etMinHumidity, etMaxHumidity;
    private TextInputEditText etNeededPrecipitation, etMaxWind, etSowingDepth;
    private TextInputEditText etDaysToGermination, etDaysToHarvest;
    private TextInputEditText etWateringInterval, etFertilizingInterval, etSoilCareInterval, etProtectionInterval;

    private TextInputLayout tilPlantName;
    private CheckBox cbCanSeedlings, cbCanDirectSow;
    private MaterialButton btnSave, btnDelete;
    private ImageView btnBack, ivSelectedPhoto;
    private View llPhotoPlaceholder;
    private TextView tvTitle, tvSubtitle;

    private ApiService apiService;
    private Integer cropId;
    private IndividualUserCrop currentCrop;
    private String selectedPhotoUri = "";

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
                    }
                }
        );
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnDelete.setOnClickListener(v -> {
            if (currentCrop != null) showDeleteConfirmation();
        });
        btnSave.setOnClickListener(v -> {
            if (validateFields()) updatePlant();
        });
        findViewById(R.id.btnSelectPhoto).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            photoLauncher.launch(intent);
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
        tvTitle.setText("Редактирование растения");
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

        if (currentCrop.getLocalPhotoPath() != null && !currentCrop.getLocalPhotoPath().isEmpty()) {
            try {
                Picasso.get().load(Uri.parse(currentCrop.getLocalPhotoPath())).into(ivSelectedPhoto);
                llPhotoPlaceholder.setVisibility(View.GONE);
            } catch (Exception e) {
                Log.e("EditPlant", "Error loading image", e);
            }
        }
    }

    private boolean validateFields() {
        if (TextUtils.isEmpty(etPlantName.getText().toString().trim())) {
            tilPlantName.setError("Укажите название");
            return false;
        }
        tilPlantName.setError(null);
        return true;
    }

    private void updatePlant() {
        if (currentCrop == null) return;

        currentCrop.setName(etPlantName.getText().toString().trim());
        currentCrop.setVariety(etVariety.getText().toString().trim());
        currentCrop.setDescription(etDescription.getText().toString().trim());

        currentCrop.setMinTemp(parseShort(etMinTemp));
        currentCrop.setMaxTemp(parseShort(etMaxTemp));
        currentCrop.setMinHumidity(parseInteger(etMinHumidity));
        currentCrop.setMaxHumidity(parseInteger(etMaxHumidity));
        currentCrop.setNeededPrecipitation(parseShort(etNeededPrecipitation));
        currentCrop.setMaxWind(parseShort(etMaxWind));
        currentCrop.setSowingDepth(parseInteger(etSowingDepth));
        currentCrop.setDaysToGermination(parseInteger(etDaysToGermination));
        currentCrop.setDaysToHarvest(parseInteger(etDaysToHarvest));
        currentCrop.setWateringInterval(parseInteger(etWateringInterval));
        currentCrop.setFertilizingInterval(parseInteger(etFertilizingInterval));
        currentCrop.setSoilCareInterval(parseInteger(etSoilCareInterval));
        currentCrop.setProtectionInterval(parseInteger(etProtectionInterval));
        currentCrop.setCanSeedlings(cbCanSeedlings.isChecked());
        currentCrop.setCanDirectSow(cbCanDirectSow.isChecked());

        apiService.updateUserCrop(cropId, currentCrop).enqueue(new Callback<IndividualUserCrop>() {
            @Override
            public void onResponse(Call<IndividualUserCrop> call, Response<IndividualUserCrop> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EditPlantActivityUser.this, "Сохранено", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditPlantActivityUser.this, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<IndividualUserCrop> call, Throwable t) {
                Toast.makeText(EditPlantActivityUser.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
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
        apiService.deleteUserCrop(cropId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Toast.makeText(EditPlantActivityUser.this, "Удалено", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(EditPlantActivityUser.this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setVal(TextInputEditText et, Object val) {
        if (val != null && et != null) {
            et.setText(String.valueOf(val));
        }
    }

    private Float parseFloat(TextInputEditText et) {
        if (et == null || TextUtils.isEmpty(et.getText().toString().trim())) return null;
        try {
            return Float.parseFloat(et.getText().toString().trim());
        } catch (Exception e) {
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