package com.example.ars;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import com.example.ars.utils.SharedPreferencesHelper;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlantDetailActivity extends AppCompatActivity {

    private ApiService apiService;
    private int recordId;
    private boolean isIndividual;
    private SharedPreferencesHelper prefsHelper;

    private TextView tvName, tvDesc;
    private TextView tvValTemp, tvValHum, tvValWind, tvValPrec, tvValDepth,
            tvValSeedlings, tvValDirectSow, tvValGerm, tvValHarvest,
            tvValWaterInt, tvValFertInt, tvValSoilInt, tvValCategory, tvValProtect;
    private View labelCategory;
    private ImageView ivPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_detail);

        prefsHelper = new SharedPreferencesHelper(this);
        RetrofitClient.initialize(prefsHelper);
        apiService = RetrofitClient.getApiService();

        int userCropId = getIntent().getIntExtra("user_crop_id", -1);
        int individualCropId = getIntent().getIntExtra("individual_crop_id", -1);
        isIndividual = getIntent().getBooleanExtra("is_individual", false);

        Log.d("PlantDetail", "userCropId: " + userCropId);
        Log.d("PlantDetail", "individualCropId: " + individualCropId);
        Log.d("PlantDetail", "isIndividual: " + isIndividual);

        if (isIndividual && individualCropId != -1) {
            recordId = individualCropId;
        } else if (userCropId != -1) {
            recordId = userCropId;
        } else {
            Toast.makeText(this, "Ошибка: ID растения не передан", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d("PlantDetail", "ID: " + recordId + ", isIndividual: " + isIndividual);

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
        labelCategory = findViewById(R.id.labelCategory);
    }

    private void loadData() {
        if (isIndividual) {
            // Для пользовательского растения - запрос к /api/my-crops/{id}
            Log.d("PlantDetail", "Загружаем пользовательское растение с ID: " + recordId);

            apiService.getUserCropById(recordId).enqueue(new Callback<IndividualUserCrop>() {
                @Override
                public void onResponse(Call<IndividualUserCrop> call, Response<IndividualUserCrop> response) {
                    Log.d("PlantDetail", "Код ответа: " + response.code());
                    if (response.isSuccessful() && response.body() != null) {
                        renderIndividual(response.body());
                    } else {
                        Log.e("PlantDetail", "Ошибка: " + response.code());
                        Toast.makeText(PlantDetailActivity.this, "Растение не найдено", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<IndividualUserCrop> call, Throwable t) {
                    Log.e("PlantDetail", "Ошибка: " + t.getMessage());
                    Toast.makeText(PlantDetailActivity.this, "Ошибка загрузки", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Для системного растения - нужно получить через userCrop
            Log.d("PlantDetail", "Загружаем системное растение с userCropId: " + recordId);

            if (prefsHelper.getUser() == null) return;

            apiService.getUserCrops(prefsHelper.getUser().getId()).enqueue(new Callback<List<UserCrop>>() {
                @Override
                public void onResponse(Call<List<UserCrop>> call, Response<List<UserCrop>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (UserCrop uc : response.body()) {
                            if (uc.getId() == recordId) {
                                renderSystem(uc);
                                return;
                            }
                        }
                        Toast.makeText(PlantDetailActivity.this, "Растение не найдено", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<List<UserCrop>> call, Throwable t) {}
            });
        }
    }

    private void renderIndividual(IndividualUserCrop crop) {
        Log.d("PLANT_DEBUG", "=== renderIndividual ВЫЗВАН ===");

        // Название + сорт
        String displayName = crop.getName();
        if (crop.getVariety() != null && !crop.getVariety().isEmpty()) {
            displayName += " (" + crop.getVariety() + ")";
        }
        tvName.setText(displayName);
        tvDesc.setText(crop.getDescription() != null ? crop.getDescription() : "Описание отсутствует");

        // Температура
        if (crop.getMinTemp() != null && crop.getMaxTemp() != null) {
            tvValTemp.setText(crop.getMinTemp() + " - " + crop.getMaxTemp() + " °C");
        } else if (crop.getMinTemp() != null) {
            tvValTemp.setText("от " + crop.getMinTemp() + " °C");
        } else if (crop.getMaxTemp() != null) {
            tvValTemp.setText("до " + crop.getMaxTemp() + " °C");
        } else {
            tvValTemp.setText("Не указана");
        }

        // Влажность
        if (crop.getMinHumidity() != null && crop.getMaxHumidity() != null) {
            tvValHum.setText(crop.getMinHumidity() + " - " + crop.getMaxHumidity() + " %");
        } else if (crop.getMinHumidity() != null) {
            tvValHum.setText("от " + crop.getMinHumidity() + " %");
        } else if (crop.getMaxHumidity() != null) {
            tvValHum.setText("до " + crop.getMaxHumidity() + " %");
        } else {
            tvValHum.setText("Не указана");
        }

        // Ветер
        tvValWind.setText(crop.getMaxWind() != null ? crop.getMaxWind() + " м/с" : "--");

        // Осадки
        tvValPrec.setText(crop.getNeededPrecipitation() != null ? crop.getNeededPrecipitation() + " мм" : "--");

        // Глубина посева
        tvValDepth.setText(crop.getSowingDepth() != null ? crop.getSowingDepth() + " см" : "--");

        // Рассада/грунт
        tvValSeedlings.setText(crop.getCanSeedlings() != null && crop.getCanSeedlings() ? "Да" : "Нет");
        tvValDirectSow.setText(crop.getCanDirectSow() != null && crop.getCanDirectSow() ? "Да" : "Нет");

        // Дни до всходов/урожая
        tvValGerm.setText(crop.getDaysToGermination() != null ? crop.getDaysToGermination() + " дн." : "--");
        tvValHarvest.setText(crop.getDaysToHarvest() != null ? crop.getDaysToHarvest() + " дн." : "--");

        // Интервалы ухода
        tvValWaterInt.setText(crop.getWateringInterval() != null ? crop.getWateringInterval() + " дн." : "--");
        tvValFertInt.setText(crop.getFertilizingInterval() != null ? crop.getFertilizingInterval() + " дн." : "--");
        tvValSoilInt.setText(crop.getSoilCareInterval() != null ? crop.getSoilCareInterval() + " дн." : "--");
        tvValProtect.setText(crop.getProtectionInterval() != null ? crop.getProtectionInterval() + " дн." : "--");

        // Категория
        if (crop.getCategoryId() != null && crop.getCategoryId() > 0) {
            loadCategoryName(crop.getCategoryId());
        } else {
            tvValCategory.setText("Не указана");
        }
    }

    private void renderSystem(UserCrop uc) {
        Log.d("PLANT_DEBUG", "=== renderSystem ВЫЗВАН ===");

        if (uc.getCrop() == null) {
            Log.d("PLANT_DEBUG", "uc.getCrop() = null");
            return;
        }

        Crop c = uc.getCrop();

        // Название + сорт
        String displayName = c.getName();
        if (c.getVariety() != null && !c.getVariety().isEmpty()) {
            displayName += " (" + c.getVariety() + ")";
        }
        tvName.setText(displayName);
        tvDesc.setText(c.getDescription() != null ? c.getDescription() : "Описание отсутствует");

        // Температура
        if (c.getMinTemp() != null && c.getMaxTemp() != null) {
            tvValTemp.setText(c.getMinTemp() + " - " + c.getMaxTemp() + " °C");
        } else if (c.getMinTemp() != null) {
            tvValTemp.setText("от " + c.getMinTemp() + " °C");
        } else if (c.getMaxTemp() != null) {
            tvValTemp.setText("до " + c.getMaxTemp() + " °C");
        } else {
            tvValTemp.setText("Не указана");
        }

        if (c.getMinHumidity() != null && c.getMaxHumidity() != null) {
            tvValHum.setText(c.getMinHumidity() + " - " + c.getMaxHumidity() + " %");
        } else if (c.getMinHumidity() != null) {
            tvValHum.setText("от " + c.getMinHumidity() + " %");
        } else if (c.getMaxHumidity() != null) {
            tvValHum.setText("до " + c.getMaxHumidity() + " %");
        } else {
            tvValHum.setText("Не указана");
        }

        tvValWind.setText(c.getMaxWind() != null ? c.getMaxWind() + " м/с" : "--");
        tvValPrec.setText(c.getNeededPrecipitation() != null ? c.getNeededPrecipitation() + " мм" : "--");
        tvValDepth.setText(c.getSowingDepth() != null ? c.getSowingDepth() + " см" : "--");
        tvValSeedlings.setText(c.getCanSeedlings() != null && c.getCanSeedlings() ? "Да" : "Нет");
        tvValDirectSow.setText(c.getCanDirectSow() != null && c.getCanDirectSow() ? "Да" : "Нет");
        tvValGerm.setText(c.getDaysToGermination() != null ? c.getDaysToGermination() + " дн." : "--");
        tvValHarvest.setText(c.getDaysToHarvest() != null ? c.getDaysToHarvest() + " дн." : "--");
        tvValWaterInt.setText(c.getWateringInterval() != null ? c.getWateringInterval() + " дн." : "--");
        tvValFertInt.setText(c.getFertilizingInterval() != null ? c.getFertilizingInterval() + " дн." : "--");
        tvValSoilInt.setText(c.getSoilCareInterval() != null ? c.getSoilCareInterval() + " дн." : "--");
        tvValProtect.setText(c.getProtectionInterval() != null ? c.getProtectionInterval() + " дн." : "--");

        tvValCategory.setText(c.getCategory() != null ? c.getCategory() : "--");
    }

    private void loadCategoryName(int categoryId) {
        apiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Category category : response.body()) {
                        if (category.getId() == categoryId) {
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
    private void showConfirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Удаление")
                .setMessage("Вы уверены, что хотите удалить это растение?")
                .setPositiveButton("Удалить", (d, w) -> deleteProcess())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteProcess() {
        if (isIndividual) {
            Log.d("DELETE_DEBUG", "Удаляем пользовательское растение с ID: " + recordId);

            apiService.deleteUserCrop(recordId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {

                    if (response.isSuccessful()) {
                        Toast.makeText(PlantDetailActivity.this, "Растение удалено", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Log.e("DELETE_DEBUG", "Ошибка: " + response.code());
                        try {
                            String error = response.errorBody().string();
                            Toast.makeText(PlantDetailActivity.this, "Ошибка: " + error, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(PlantDetailActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(PlantDetailActivity.this, "Ошибка: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            if (prefsHelper.getUser() == null) return;

            int userId = prefsHelper.getUser().getId();

            apiService.deleteUserCrop(userId, recordId).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {


                    if (response.isSuccessful() && response.body() != null) {
                        Boolean success = (Boolean) response.body().get("success");
                        Log.d("DELETE_DEBUG", "success = " + success);

                        if (success != null && success) {
                            Toast.makeText(PlantDetailActivity.this, "Растение удалено", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(PlantDetailActivity.this, "Ошибка: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}