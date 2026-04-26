package com.example.ars;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
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

    private TextView tvName, tvDesc, tvRecs;
    private ImageView ivPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_detail);

        apiService = RetrofitClient.getApiService();

        // Получаем данные из интента
        recordId = getIntent().getIntExtra("user_crop_id", -1);
        isIndividual = getIntent().getBooleanExtra("is_individual", false);

        tvName = findViewById(R.id.tvPlantName);
        tvDesc = findViewById(R.id.tvDescription);
        tvRecs = findViewById(R.id.tvRecommendations);
        ivPhoto = findViewById(R.id.ivPlantPhoto);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnDelete).setOnClickListener(v -> showConfirmDelete());

        loadData();
    }

    private void loadData() {
        if (isIndividual) {
            // Запрос для личного растения (Монджарик)
            apiService.getUserCropById(recordId).enqueue(new Callback<IndividualUserCrop>() {
                @Override
                public void onResponse(Call<IndividualUserCrop> call, Response<IndividualUserCrop> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        renderIndividual(response.body());
                    }
                }
                @Override public void onFailure(Call<IndividualUserCrop> call, Throwable t) {
                    Toast.makeText(PlantDetailActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Запрос для системного (Помидор)
            SharedPreferencesHelper helper = new SharedPreferencesHelper(this);
            apiService.getUserCrops(helper.getUser().getId()).enqueue(new Callback<List<UserCrop>>() {
                @Override
                public void onResponse(Call<List<UserCrop>> call, Response<List<UserCrop>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (UserCrop uc : response.body()) {
                            if (uc.getId() == recordId) {
                                renderSystem(uc);
                                return;
                            }
                        }
                    }
                }
                @Override public void onFailure(Call<List<UserCrop>> call, Throwable t) {}
            });
        }
    }

    private void renderIndividual(IndividualUserCrop crop) {
        tvName.setText(crop.getName());
        tvDesc.setText(crop.getDescription());

        // ФОТО: Пробуем загрузить локальный Uri
        if (crop.getLocalPhotoPath() != null && !crop.getLocalPhotoPath().isEmpty()) {
            Picasso.get().load(Uri.parse(crop.getLocalPhotoPath()))
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(ivPhoto);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📋 ТИП: МОЁ РАСТЕНИЕ\n\n");
        // Выводим ВООБЩЕ ВСЕ параметры, которые есть в модели
        addP(sb, "Температура", crop.getMinTemp(), " - ", crop.getMaxTemp(), "°C");
        addP(sb, "Влажность", crop.getMinHumidity(), " - ", crop.getMaxHumidity(), "%");
        addP(sb, "Ветер (макс)", crop.getMaxWind(), " м/с");
        addP(sb, "Осадки (мин)", crop.getNeededPrecipitation(), " мм");
        addP(sb, "Глубина посадки", crop.getSowingDepth(), " см");
        addP(sb, "Дней до всходов", crop.getDaysToGermination(), "");
        addP(sb, "Дней до урожая", crop.getDaysToHarvest(), "");

        tvRecs.setText(sb.toString());
    }

    private void renderSystem(UserCrop uc) {
        if (uc.getCrop() == null) return;
        tvName.setText(uc.getCrop().getName());
        tvDesc.setText(uc.getCrop().getDescription());

        // ФОТО: Ссылка на сервер
        String path = uc.getCrop().getPhotoPath();
        if (path != null) {
            if (path.startsWith("/")) path = path.substring(1);
            String url = RetrofitClient.BASE_URL + "/api/img/" + path;
            Picasso.get().load(url).placeholder(R.drawable.ic_profile).into(ivPhoto);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🌱 КАТЕГОРИЯ: ").append(uc.getCrop().getCategory()).append("\n\n");
        addP(sb, "Температура", uc.getCrop().getMinTemp(), " - ", uc.getCrop().getMaxTemp(), "°C");
        addP(sb, "Влажность", uc.getCrop().getMinHumidity(), " - ", uc.getCrop().getMaxHumidity(), "%");
        addP(sb, "Ветер (макс)", uc.getCrop().getMaxWind(), " м/с");
        addP(sb, "Осадки (мин)", uc.getCrop().getNeededPrecipitation(), " мм");
        addP(sb, "Глубина", uc.getCrop().getSowingDepth(), " см");
        addP(sb, "Срок урожая", uc.getCrop().getDaysToHarvest(), " дней");

        tvRecs.setText(sb.toString());
    }

    private void addP(StringBuilder sb, String label, Object v1, String mid, Object v2, String unit) {
        if (v1 != null || v2 != null) {
            sb.append("• ").append(label).append(": ");
            if (v1 != null) sb.append(v1);
            if (v1 != null && v2 != null) sb.append(mid);
            if (v2 != null) sb.append(v2);
            sb.append(unit).append("\n\n");
        }
    }

    private void addP(StringBuilder sb, String label, Object v, String unit) {
        if (v != null) sb.append("• ").append(label).append(": ").append(v).append(unit).append("\n\n");
    }

    private void showConfirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Удаление")
                .setMessage("Вы уверены, что хотите удалить растение?")
                .setPositiveButton("Удалить", (d, w) -> deleteProcess())
                .setNegativeButton("Отмена", null).show();
    }

    private void deleteProcess() {
        if (isIndividual) {
            // Удаление ЛИЧНОГО (метод с 1 параметром)
            apiService.deleteUserCrop(recordId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        setResult(RESULT_OK);
                        finish();
                    }
                }
                @Override public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(PlantDetailActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Удаление СИСТЕМНОГО (метод с 2 параметрами userId и cropId)
            SharedPreferencesHelper helper = new SharedPreferencesHelper(this);
            int userId = helper.getUser().getId();

            apiService.deleteUserCrop(userId, recordId).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful()) {
                        setResult(RESULT_OK);
                        finish();
                    }
                }
                @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    Toast.makeText(PlantDetailActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}