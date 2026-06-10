package com.example.ars;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.Category;
import com.example.ars.models.Crop;
import com.example.ars.models.IndividualUserCrop;
import com.example.ars.models.UserCrop;
import com.example.ars.models.UserCategory;
import com.example.ars.utils.SharedPreferencesHelper;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlantDetailActivity extends AppCompatActivity {

    private ApiService apiService;
    private int userCropRowId;
    private boolean isIndividual;
    private SharedPreferencesHelper prefsHelper;
    private UserCrop currentUserCrop;

    private TextView tvName, tvDesc;
    private TextView tvValTemp, tvValHum, tvValWind, tvValPrec, tvValDepth,
            tvValSeedlings, tvValDirectSow, tvValGerm, tvValHarvest,
            tvValWaterInt, tvValFertInt, tvValSoilInt, tvValCategory, tvValProtect;
    private TextView tvLocation;
    private ImageView ivPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_detail);

        prefsHelper = new SharedPreferencesHelper(this);
        RetrofitClient.initialize(prefsHelper);
        apiService = RetrofitClient.getApiService();

        userCropRowId = getIntent().getIntExtra("user_crop_id", -1);
        int individualCropId = getIntent().getIntExtra("individual_crop_id", -1);
        isIndividual = getIntent().getBooleanExtra("is_individual", false);

        if (userCropRowId == -1) {
            Toast.makeText(this, "Ошибка: ID растения не передан", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnDelete).setOnClickListener(v -> showConfirmDelete());

        loadData();
    }

    private void initViews() {
        tvName = findViewById(R.id.tvPlantName);
        tvDesc = findViewById(R.id.tvDescription);
        ivPhoto = findViewById(R.id.ivPlantPhoto);

        tvValTemp = findViewById(R.id.tvValTemp);
        tvValHum = findViewById(R.id.tvValHum);
        tvValWind = findViewById(R.id.tvValWind);
        tvValPrec = findViewById(R.id.tvValPrec);
        tvValDepth = findViewById(R.id.tvValDepth);
        tvValSeedlings = findViewById(R.id.tvValSeedlings);
        tvValDirectSow = findViewById(R.id.tvValDirectSow);
        tvValGerm = findViewById(R.id.tvValGerm);
        tvValHarvest = findViewById(R.id.tvValHarvest);
        tvValWaterInt = findViewById(R.id.tvValWaterInt);
        tvValFertInt = findViewById(R.id.tvValFertInt);
        tvValSoilInt = findViewById(R.id.tvValSoilInt);
        tvValProtect = findViewById(R.id.tvValProtect);
        tvValCategory = findViewById(R.id.tvValCategory);

        tvLocation = findViewById(R.id.tvLocation);
    }

    private void loadData() {
        if (prefsHelper.getUser() == null) return;

        apiService.getUserCrops(prefsHelper.getUser().getId()).enqueue(new Callback<List<UserCrop>>() {
            @Override
            public void onResponse(Call<List<UserCrop>> call, Response<List<UserCrop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (UserCrop uc : response.body()) {
                        if (uc.getId().equals(userCropRowId)) {
                            currentUserCrop = uc;
                            if (isIndividual && uc.getIndividualCrop() != null) {
                                renderIndividual(uc);
                            } else if (!isIndividual && uc.getCrop() != null) {
                                renderSystem(uc);
                            } else {
                                Toast.makeText(PlantDetailActivity.this, "Данные растения не найдены", Toast.LENGTH_SHORT).show();
                            }
                            return;
                        }
                    }
                    Toast.makeText(PlantDetailActivity.this, "Растение не найдено", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<UserCrop>> call, Throwable t) {
                Toast.makeText(PlantDetailActivity.this, "Ошибка загрузки", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderIndividual(UserCrop uc) {
        IndividualUserCrop crop = uc.getIndividualCrop();

        String displayName = crop.getName();
        if (crop.getVariety() != null && !crop.getVariety().isEmpty()) {
            displayName += " (" + crop.getVariety() + ")";
        }
        tvName.setText(displayName);
        tvDesc.setText(crop.getDescription() != null ? crop.getDescription() : "Описание отсутствует");

        if (tvLocation != null) {
            String location = "";
            if (uc.getArea() != null) {
                location = uc.getArea().getName();
                if (uc.getGarden() != null) {
                    location += " - " + uc.getGarden().getName();
                }
            } else {
                location = "Не указан";
            }
            tvLocation.setText(location);
            tvLocation.setVisibility(View.VISIBLE);
        }

        if (crop.getLocalPhotoPath() != null && !crop.getLocalPhotoPath().isEmpty()) {
            String photoUrl = crop.getLocalPhotoPath();
            if (!photoUrl.startsWith("http")) {
                photoUrl = RetrofitClient.BASE_URL + photoUrl;
            }
            Picasso.get()
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_plant)
                    .error(R.drawable.ic_plant)
                    .into(ivPhoto);
        } else {
            ivPhoto.setImageResource(R.drawable.ic_plant);
        }

        if (crop.getMinTemp() != null && crop.getMaxTemp() != null) {
            tvValTemp.setText(crop.getMinTemp() + " - " + crop.getMaxTemp() + " °C");
        } else {
            tvValTemp.setText("Не указана");
        }

        if (crop.getMinHumidity() != null && crop.getMaxHumidity() != null) {
            tvValHum.setText(crop.getMinHumidity() + " - " + crop.getMaxHumidity() + " %");
        } else {
            tvValHum.setText("Не указана");
        }

        tvValWind.setText(crop.getMaxWind() != null ? crop.getMaxWind() + " м/с" : "--");
        tvValPrec.setText(crop.getNeededPrecipitation() != null ? crop.getNeededPrecipitation() + " мм" : "--");
        tvValDepth.setText(crop.getSowingDepth() != null ? crop.getSowingDepth() + " см" : "--");
        tvValSeedlings.setText(crop.getCanSeedlings() != null && crop.getCanSeedlings() ? "Да" : "Нет");
        tvValDirectSow.setText(crop.getCanDirectSow() != null && crop.getCanDirectSow() ? "Да" : "Нет");
        tvValGerm.setText(crop.getDaysToGermination() != null ? crop.getDaysToGermination() + " дн." : "--");
        tvValHarvest.setText(crop.getDaysToHarvest() != null ? crop.getDaysToHarvest() + " дн." : "--");
        tvValWaterInt.setText(crop.getWateringInterval() != null ? crop.getWateringInterval() + " дн." : "--");
        tvValFertInt.setText(crop.getFertilizingInterval() != null ? crop.getFertilizingInterval() + " дн." : "--");
        tvValSoilInt.setText(crop.getSoilCareInterval() != null ? crop.getSoilCareInterval() + " дн." : "--");
        tvValProtect.setText(crop.getProtectionInterval() != null ? crop.getProtectionInterval() + " дн." : "--");

        if (crop.getCategoryId() != null && crop.getCategoryId() > 0) {
            loadSystemCategoryName(crop.getCategoryId());
        } else if (crop.getUserCategoryId() != null && crop.getUserCategoryId() > 0) {
            loadUserCategoryName(crop.getUserCategoryId());
        } else {
            tvValCategory.setText("Не указана");
        }
    }

    private void renderSystem(UserCrop uc) {
        Crop crop = uc.getCrop();

        String displayName = crop.getName();
        if (crop.getVariety() != null && !crop.getVariety().isEmpty()) {
            displayName += " (" + crop.getVariety() + ")";
        }
        tvName.setText(displayName);
        tvDesc.setText(crop.getDescription() != null ? crop.getDescription() : "Описание отсутствует");

        if (tvLocation != null) {
            String location = "";
            if (uc.getArea() != null) {
                location = uc.getArea().getName();
                if (uc.getGarden() != null) {
                    location += " - " + uc.getGarden().getName();
                }
            } else {
                location = "Не указан";
            }
            tvLocation.setText(location);
            tvLocation.setVisibility(View.VISIBLE);
        }

        if (crop.getPhotoPath() != null && !crop.getPhotoPath().isEmpty()) {
            String photoUrl = crop.getPhotoPath();
            if (!photoUrl.startsWith("http")) {
                photoUrl = RetrofitClient.BASE_URL + photoUrl;
            }
            Picasso.get()
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_plant)
                    .error(R.drawable.ic_plant)
                    .into(ivPhoto);
        } else {
            ivPhoto.setImageResource(R.drawable.ic_plant);
        }

        if (crop.getMinTemp() != null && crop.getMaxTemp() != null) {
            tvValTemp.setText(crop.getMinTemp() + " - " + crop.getMaxTemp() + " °C");
        } else {
            tvValTemp.setText("Не указана");
        }

        if (crop.getMinHumidity() != null && crop.getMaxHumidity() != null) {
            tvValHum.setText(crop.getMinHumidity() + " - " + crop.getMaxHumidity() + " %");
        } else {
            tvValHum.setText("Не указана");
        }

        tvValWind.setText(crop.getMaxWind() != null ? crop.getMaxWind() + " м/с" : "--");
        tvValPrec.setText(crop.getNeededPrecipitation() != null ? crop.getNeededPrecipitation() + " мм" : "--");
        tvValDepth.setText(crop.getSowingDepth() != null ? crop.getSowingDepth() + " см" : "--");
        tvValSeedlings.setText(crop.getCanSeedlings() != null && crop.getCanSeedlings() ? "Да" : "Нет");
        tvValDirectSow.setText(crop.getCanDirectSow() != null && crop.getCanDirectSow() ? "Да" : "Нет");
        tvValGerm.setText(crop.getDaysToGermination() != null ? crop.getDaysToGermination() + " дн." : "--");
        tvValHarvest.setText(crop.getDaysToHarvest() != null ? crop.getDaysToHarvest() + " дн." : "--");
        tvValWaterInt.setText(crop.getWateringInterval() != null ? crop.getWateringInterval() + " дн." : "--");
        tvValFertInt.setText(crop.getFertilizingInterval() != null ? crop.getFertilizingInterval() + " дн." : "--");
        tvValSoilInt.setText(crop.getSoilCareInterval() != null ? crop.getSoilCareInterval() + " дн." : "--");
        tvValProtect.setText(crop.getProtectionInterval() != null ? crop.getProtectionInterval() + " дн." : "--");

        if (crop.getCategoryObject() != null) {
            tvValCategory.setText(crop.getCategoryObject().getName());
        } else if (crop.getCategory() != null && !crop.getCategory().isEmpty()) {
            tvValCategory.setText(crop.getCategory());
        } else {
            tvValCategory.setText("Не указана");
        }
    }

    private void loadSystemCategoryName(int categoryId) {
        apiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Category category : response.body()) {
                        if (category.getId().equals(categoryId)) {
                            tvValCategory.setText(category.getName());
                            return;
                        }
                    }
                    tvValCategory.setText("Не найдена");
                } else {
                    tvValCategory.setText("Ошибка");
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                tvValCategory.setText("Ошибка");
            }
        });
    }

    private void loadUserCategoryName(int userCategoryId) {
        apiService.getUserCategories(prefsHelper.getUser().getId()).enqueue(new Callback<List<UserCategory>>() {
            @Override
            public void onResponse(Call<List<UserCategory>> call, Response<List<UserCategory>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (UserCategory category : response.body()) {
                        if (category.getId().equals(userCategoryId)) {
                            tvValCategory.setText(category.getName());
                            return;
                        }
                    }
                    tvValCategory.setText("Не найдена");
                } else {
                    tvValCategory.setText("Ошибка");
                }
            }

            @Override
            public void onFailure(Call<List<UserCategory>> call, Throwable t) {
                tvValCategory.setText("Ошибка");
            }
        });
    }

    private void showConfirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Удаление")
                .setMessage("Вы уверены, что хотите удалить это растение из своего списка?")
                .setPositiveButton("Удалить", (d, w) -> deleteProcess())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteProcess() {
        if (prefsHelper.getUser() == null) {
            Toast.makeText(this, "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userCropRowId == -1) {
            Toast.makeText(this, "Ошибка: не передан ID растения", Toast.LENGTH_SHORT).show();
            return;
        }

        int userId = prefsHelper.getUser().getId();

        apiService.deleteUserCrop(userId, userCropRowId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Boolean success = (Boolean) response.body().get("success");
                    if (success != null && success) {
                        Toast.makeText(PlantDetailActivity.this, "Растение удалено из вашего списка", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        String error = (String) response.body().get("error");
                        Toast.makeText(PlantDetailActivity.this, "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(PlantDetailActivity.this, "Ошибка удаления: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(PlantDetailActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}