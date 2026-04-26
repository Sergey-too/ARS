package com.example.ars;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.example.ars.utils.SharedPreferencesHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddPlantActivityUser extends AppCompatActivity {

    private ApiService apiService;
    private SharedPreferencesHelper prefsHelper;

    private TextInputEditText etName, etDescription, etMinTemp, etMaxTemp, etMinHumidity,
            etMaxHumidity, etPrecipitation, etMaxWind, etSowingDepth, etGermination, etHarvest;
    private TextInputLayout tilCategory;
    private AutoCompleteTextView actvCategory;
    private ImageView ivSelectedPhoto;
    private View llPhotoPlaceholder;

    // Данные
    private List<Category> categories = new ArrayList<>();
    private Integer selectedCategoryId = null;
    private String selectedPhotoUri = "";
    private Integer editingCropId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_plant_user);

        initViews();

        apiService = RetrofitClient.getApiService();
        prefsHelper = new SharedPreferencesHelper(this);

        // Кнопка назад
        findViewById(R.id.btnBack).setOnClickListener(v -> showExitDialog());

        // Загрузка категорий с сервера
        loadCategories();

        // Проверка на редактирование
        if (getIntent().hasExtra("CROP_ID")) {
            editingCropId = getIntent().getIntExtra("CROP_ID", -1);
            loadCropData(editingCropId);
            ((MaterialButton) findViewById(R.id.btnAddPlant)).setText("Сохранить изменения");
        }

        // Выбор фото
        ActivityResultLauncher<Intent> photoLauncher = registerForActivityResult(
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

        findViewById(R.id.btnSelectPhoto).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            photoLauncher.launch(intent);
        });

        // Кнопка сохранения
        findViewById(R.id.btnAddPlant).setOnClickListener(v -> savePlant());
    }

    private void initViews() {
        etName = findViewById(R.id.etPlantName);
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

        tilCategory = findViewById(R.id.tilCategory);
        actvCategory = findViewById(R.id.actvCategory);

        ivSelectedPhoto = findViewById(R.id.ivSelectedPhoto);
        llPhotoPlaceholder = findViewById(R.id.llPhotoPlaceholder);
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
                Toast.makeText(AddPlantActivityUser.this, "Ошибка категорий", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCategoryDropdown() {
        List<String> names = new ArrayList<>();
        for (Category c : categories) names.add(c.getName());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, names);
        actvCategory.setAdapter(adapter);

        actvCategory.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategoryId = Math.toIntExact(categories.get(position).getId());
            tilCategory.setError(null);
        });
    }

    private void savePlant() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            etName.setError("Введите название");
            return;
        }
        if (selectedCategoryId == null) {
            tilCategory.setError("Выберите категорию");
            return;
        }

        IndividualUserCrop crop = new IndividualUserCrop();
        crop.setUserId(prefsHelper.getUser().getId());
        crop.setName(name);
        crop.setCategoryId(selectedCategoryId);
        crop.setDescription(etDescription.getText().toString());
        crop.setMinTemp(parseViewDouble(etMinTemp));
        crop.setMaxTemp(parseViewDouble(etMaxTemp));
        crop.setMinHumidity(parseViewDouble(etMinHumidity));
        crop.setMaxHumidity(parseViewDouble(etMaxHumidity));
        crop.setNeededPrecipitation(parseViewDouble(etPrecipitation));
        crop.setMaxWind(parseViewDouble(etMaxWind));
        crop.setSowingDepth(parseViewDouble(etSowingDepth));
        crop.setDaysToGermination(parseViewInt(etGermination));
        crop.setDaysToHarvest(parseViewInt(etHarvest));
        crop.setLocalPhotoPath(selectedPhotoUri);

        if (editingCropId == null) {
            apiService.createUserCrop(crop).enqueue(new Callback<IndividualUserCrop>() {
                @Override
                public void onResponse(Call<IndividualUserCrop> call, Response<IndividualUserCrop> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AddPlantActivityUser.this, "Сохранено", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
                @Override public void onFailure(Call<IndividualUserCrop> call, Throwable t) {}
            });
        } else {
            apiService.updateUserCrop(editingCropId, crop).enqueue(new Callback<IndividualUserCrop>() {
                @Override
                public void onResponse(Call<IndividualUserCrop> call, Response<IndividualUserCrop> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AddPlantActivityUser.this, "Обновлено", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
                @Override public void onFailure(Call<IndividualUserCrop> call, Throwable t) {}
            });
        }
    }

    private void loadCropData(Integer id) {
        apiService.getUserCropById(id).enqueue(new Callback<IndividualUserCrop>() {
            @Override
            public void onResponse(Call<IndividualUserCrop> call, Response<IndividualUserCrop> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fillForm(response.body());
                }
            }
            @Override public void onFailure(Call<IndividualUserCrop> call, Throwable t) {}
        });
    }

    private void fillForm(IndividualUserCrop crop) {
        etName.setText(crop.getName());
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

        selectedCategoryId = crop.getCategoryId();
        // Поиск имени категории для отображения в TextView
        if (selectedCategoryId != null) {
            for (Category c : categories) {
                if (c.getId().equals(selectedCategoryId)) {
                    actvCategory.setText(c.getName(), false);
                    break;
                }
            }
        }

        if (crop.getLocalPhotoPath() != null && !crop.getLocalPhotoPath().isEmpty()) {
            selectedPhotoUri = crop.getLocalPhotoPath();
            ivSelectedPhoto.setImageURI(Uri.parse(selectedPhotoUri));
            llPhotoPlaceholder.setVisibility(View.GONE);
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

    private Double parseViewDouble(TextInputEditText view) {
        String val = view.getText().toString().trim();
        return val.isEmpty() ? null : Double.parseDouble(val);
    }

    private Integer parseViewInt(TextInputEditText view) {
        String val = view.getText().toString().trim();
        return val.isEmpty() ? null : Integer.parseInt(val);
    }
}