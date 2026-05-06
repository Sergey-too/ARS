package com.example.ars;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
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

        apiService = RetrofitClient.getApiService();

        // Получаем ID записи и флаг типа растения
        recordId = getIntent().getIntExtra("user_crop_id", -1);
        isIndividual = getIntent().getBooleanExtra("is_individual", false);

        initViews();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnDelete).setOnClickListener(v -> showConfirmDelete());

        loadData();
    }

    private void initViews() {
        tvName = findViewById(R.id.tvPlantName);
        tvDesc = findViewById(R.id.tvDescription);
        ivPhoto = findViewById(R.id.ivPlantPhoto);

        // Поля характеристик (согласно новым ID в XML)
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
            apiService.getUserCropById(recordId).enqueue(new Callback<IndividualUserCrop>() {
                @Override
                public void onResponse(Call<IndividualUserCrop> call, Response<IndividualUserCrop> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        renderIndividual(response.body());
                    }
                }
                @Override public void onFailure(Call<IndividualUserCrop> call, Throwable t) {
                    Toast.makeText(PlantDetailActivity.this, "Ошибка загрузки", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            SharedPreferencesHelper helper = new SharedPreferencesHelper(this);
            apiService.getUserCrops(helper.getUser().getId()).enqueue(new Callback<List<UserCrop>>() {
                @Override
                public void onResponse(Call<List<UserCrop>> call, Response<List<UserCrop>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        android.util.Log.d("ARS_DEBUG", "Ищем ID: " + recordId + " в списке из " + response.body().size() + " элементов");
                        for (UserCrop uc : response.body()) {
                            android.util.Log.d("ARS_DEBUG", "Проверка объекта с ID: " + uc.getId());
                            if (uc.getId() == recordId) {
                                renderSystem(uc);
                                return;
                            }
                        }
                        android.util.Log.e("ARS_DEBUG", "Объект с ID " + recordId + " не найден в ответе сервера!");
                    }
                }
                @Override public void onFailure(Call<List<UserCrop>> call, Throwable t) {}
            });
        }
    }

    private void renderIndividual(IndividualUserCrop crop) {
        tvName.setText(crop.getName());
        tvDesc.setText(crop.getDescription() != null ? crop.getDescription() : "Описание отсутствует");

        setRangeValue(tvValTemp, crop.getMinTemp(), crop.getMaxTemp(), "°C");

        setRangeValueInt(tvValHum, crop.getMinHumidity(), crop.getMaxHumidity(), "%");

        setSingleValue(tvValWind, crop.getMaxWind(), "м/с");
        setSingleValue(tvValPrec, crop.getNeededPrecipitation(), "мм");

        setSingleValueInt(tvValDepth, crop.getSowingDepth(), "см");

        setBooleanValue(tvValSeedlings, crop.getCanSeedlings());
        setBooleanValue(tvValDirectSow, crop.getCanDirectSow());
        setSingleValueInt(tvValGerm, crop.getDaysToGermination(), "дн.");
        setSingleValueInt(tvValHarvest, crop.getDaysToHarvest(), "дн.");

        setSingleValueInt(tvValWaterInt, crop.getWateringInterval(), "дн.");
        setSingleValueInt(tvValFertInt, crop.getFertilizingInterval(), "дн.");
        setSingleValueInt(tvValSoilInt, crop.getSoilCareInterval(), "дн.");
        setSingleValueInt(tvValProtect, crop.getProtectionInterval(), "дн.");

        if (labelCategory != null) labelCategory.setVisibility(View.GONE);
        if (tvValCategory != null) tvValCategory.setVisibility(View.GONE);


//        if (crop.getLocalPhotoPath() != null && !crop.getLocalPhotoPath().isEmpty()) {
//            try {
//                Picasso.get().load(Uri.parse(crop.getLocalPhotoPath()))
//                        .placeholder(R.drawable.ic_info)
//                        .into(ivPhoto);
//                if (photoPlaceholder != null) photoPlaceholder.setVisibility(View.GONE);
//            } catch (Exception e) {
//                if (photoPlaceholder != null) photoPlaceholder.setVisibility(View.VISIBLE);
//            }
//        } else {
//            if (photoPlaceholder != null) photoPlaceholder.setVisibility(View.VISIBLE);
//        }
    }
    private void setRangeValueInt(TextView view, Integer min, Integer max, String unit) {
        if (view == null) return;
        if (min != null && max != null) {
            view.setText(min + " - " + max + unit);
        } else if (min != null) {
            view.setText("от " + min + unit);
        } else if (max != null) {
            view.setText("до " + max + unit);
        } else {
            view.setText("Не указана");
        }
    }

    private void setSingleValueInt(TextView view, Integer val, String unit) {
        if (view == null) return;
        if (val != null) {
            view.setText(val + unit);
        } else {
            view.setText("--");
        }
    }

    private void renderSystem(UserCrop uc) {
        if (uc.getCrop() == null) return;
        Crop c = uc.getCrop();

        // Название + сорт в одну строку
        String displayName = c.getName();
        if (c.getVariety() != null && !c.getVariety().isEmpty()) {
            displayName += " (" + c.getVariety() + ")";
        }
        tvName.setText(displayName);
        tvDesc.setText(c.getDescription());

        setRangeValue(tvValTemp, c.getMinTemp(), c.getMaxTemp(), " °C");
        setRangeValue(tvValHum, c.getMinHumidity(), c.getMaxHumidity(), " %");
        setSingleValue(tvValWind, c.getMaxWind(), " м/с");
        setSingleValue(tvValPrec, c.getNeededPrecipitation(), " мм");
        setSingleValue(tvValDepth, c.getSowingDepth(), " см");
        setBooleanValue(tvValSeedlings, c.getCanSeedlings());
        setBooleanValue(tvValDirectSow, c.getCanDirectSow());
        setSingleValue(tvValGerm, c.getDaysToGermination(), " дн.");
        setSingleValue(tvValHarvest, c.getDaysToHarvest(), " дн.");
        setSingleValue(tvValWaterInt, c.getWateringInterval(), " дн.");
        setSingleValue(tvValFertInt, c.getFertilizingInterval(), " дн.");
        setSingleValue(tvValSoilInt, c.getSoilCareInterval(), " дн.");
        setSingleValue(tvValProtect, c.getProtectionInterval(), " дн.");

        if (labelCategory != null) labelCategory.setVisibility(View.VISIBLE);
        if (tvValCategory != null) tvValCategory.setVisibility(View.VISIBLE);
        setSingleValue(tvValCategory, c.getCategory(), "");

        if (c.getPhotoPath() != null) {
            String path = c.getPhotoPath();
            if (path.startsWith("/")) path = path.substring(1);
            String url = RetrofitClient.BASE_URL + "/api/img/" + path;
            Picasso.get().load(url).placeholder(R.drawable.ic_info).into(ivPhoto);
        }
    }

    private void setRangeValue(TextView view, Object min, Object max, String unit) {
        if (view == null) return;
        android.util.Log.d("ARS_DEBUG", "Temp: min=" + min + ", max=" + max);

        if (min != null && max != null) {
            view.setText(min + " - " + max + unit);
        } else if (min != null) {
            view.setText("от " + min + unit);
        } else if (max != null) {
            view.setText("до " + max + unit);
        } else {
            view.setText("Не указана");
        }
    }

    private void setSingleValue(TextView view, Object val, String unit) {
        if (view == null) return;
        if (val != null && !val.toString().isEmpty()) view.setText(val + unit);
        else view.setText("--");
    }

    private void setBooleanValue(TextView view, Boolean val) {
        if (view == null) return;
        if (val == null) view.setText("--");
        else view.setText(val ? "Да" : "Нет");
    }

    private void showConfirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Удаление")
                .setMessage("Вы уверены, что хотите удалить это растение?")
                .setPositiveButton("Удалить", (d, w) -> deleteProcess())
                .setNegativeButton("Отмена", null).show();
    }

    private void deleteProcess() {
        if (isIndividual) {
            apiService.deleteUserCrop(recordId).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) { setResult(RESULT_OK); finish(); }
                }
                @Override public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(PlantDetailActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            SharedPreferencesHelper helper = new SharedPreferencesHelper(this);
            apiService.deleteUserCrop(helper.getUser().getId(), recordId).enqueue(new Callback<Map<String, Object>>() {
                @Override public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful()) { setResult(RESULT_OK); finish(); }
                }
                @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
            });
        }
    }
}