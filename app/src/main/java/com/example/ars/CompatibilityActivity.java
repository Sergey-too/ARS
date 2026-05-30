package com.example.ars;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.adapters.CompatibilityAdapter;
import com.example.ars.adapters.IndividualCompatibilityAdapter;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.CompatibilityDTO;
import com.example.ars.models.IndividualCompatibilityDTO;
import com.example.ars.models.UserCrop;
import com.example.ars.utils.SharedPreferencesHelper;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CompatibilityActivity extends AppCompatActivity {

    private RecyclerView rvCompatibility;
    private LinearLayout containerTopNames, containerLeftNames;
    private HorizontalScrollView headerScroll, dataHorizontalScroll;
    private ScrollView sideScroll;
    private AutoCompleteTextView actvCompatibilityType;
    private TextInputLayout tilCompatibilityType;

    private int currentUserId;
    private String[] modes = {"Системная совместимость", "Мои растения"};

    private List<CompatibilityDTO> commonData;
    private CompatibilityAdapter commonAdapter;

    private List<IndividualCompatibilityDTO> individualData = new ArrayList<>();
    private List<UserCrop> userCrops;
    private List<String> userCropNames;
    private IndividualCompatibilityAdapter individualAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compatibility);

        rvCompatibility = findViewById(R.id.rvCompatibility);
        containerTopNames = findViewById(R.id.containerTopNames);
        containerLeftNames = findViewById(R.id.containerLeftNames);
        headerScroll = findViewById(R.id.headerScroll);
        sideScroll = findViewById(R.id.sideScroll);
        dataHorizontalScroll = findViewById(R.id.dataHorizontalScroll);
        tilCompatibilityType = findViewById(R.id.tilCompatibilityType);
        actvCompatibilityType = findViewById(R.id.actvCompatibilityType);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(this);
        currentUserId = prefsHelper.getUser().getId();

        setupModeSpinner();
        loadCommonData();
    }

    private void setupModeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, modes);
        actvCompatibilityType.setAdapter(adapter);

        actvCompatibilityType.setOnItemClickListener((parent, view, position, id) -> {
            clearTable();
            if (position == 0) {
                loadCommonData();
            } else {
                loadUserCrops();
            }
        });
    }

    private void clearTable() {
        containerTopNames.removeAllViews();
        containerLeftNames.removeAllViews();
        rvCompatibility.setAdapter(null);
    }

    private void loadCommonData() {
        RetrofitClient.getApiService().getCompatibilityMatrix().enqueue(new Callback<List<CompatibilityDTO>>() {
            @Override
            public void onResponse(Call<List<CompatibilityDTO>> call, Response<List<CompatibilityDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    commonData = response.body();
                    setupCommonTable();
                }
            }

            @Override
            public void onFailure(Call<List<CompatibilityDTO>> call, Throwable t) {
                Toast.makeText(CompatibilityActivity.this, "Ошибка загрузки", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupCommonTable() {
        List<String> crops = commonData.stream()
                .map(CompatibilityDTO::getCrop1)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        int n = crops.size();
        int cellSize = (int) (45 * getResources().getDisplayMetrics().density);

        for (String name : crops) {
            TextView tvTop = new TextView(this);
            tvTop.setText(name);
            tvTop.setGravity(Gravity.CENTER);
            tvTop.setTextSize(10);
            tvTop.setSingleLine(true);
            tvTop.setEllipsize(null);
            LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(cellSize, 120);
            tvTop.setLayoutParams(topParams);
            tvTop.setRotation(-90);
            containerTopNames.addView(tvTop);

            TextView tvLeft = new TextView(this);
            tvLeft.setText(name);
            tvLeft.setGravity(Gravity.CENTER);
            tvLeft.setTextSize(10);
            tvLeft.setSingleLine(true);
            tvLeft.setEllipsize(null);
            tvLeft.setPadding(8, 0, 8, 0);
            LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(120, cellSize);
            tvLeft.setLayoutParams(leftParams);
            containerLeftNames.addView(tvLeft);
        }

        rvCompatibility.setLayoutManager(new GridLayoutManager(this, n));
        commonAdapter = new CompatibilityAdapter(commonData, null);
        rvCompatibility.setAdapter(commonAdapter);

        setupScrollSync();
    }

    private void setupIndividualTable() {
        if (userCrops == null || userCrops.isEmpty()) return;

        userCropNames = userCrops.stream()
                .map(UserCrop::getName)
                .sorted()
                .collect(Collectors.toList());

        int n = userCropNames.size();
        int cellSize = (int) (45 * getResources().getDisplayMetrics().density);

        for (String name : userCropNames) {
            TextView tvTop = new TextView(this);
            tvTop.setText(name);
            tvTop.setGravity(Gravity.CENTER);
            tvTop.setTextSize(10);
            tvTop.setSingleLine(true);
            tvTop.setEllipsize(null);
            LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(cellSize, 120);
            tvTop.setLayoutParams(topParams);
            tvTop.setRotation(-90);
            containerTopNames.addView(tvTop);

            TextView tvLeft = new TextView(this);
            tvLeft.setText(name);
            tvLeft.setGravity(Gravity.CENTER);
            tvLeft.setTextSize(10);
            tvLeft.setSingleLine(true);
            tvLeft.setEllipsize(null);
            tvLeft.setPadding(8, 0, 8, 0);
            LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(120, cellSize);
            tvLeft.setLayoutParams(leftParams);
            containerLeftNames.addView(tvLeft);
        }

        rvCompatibility.setLayoutManager(new GridLayoutManager(this, n));
        individualAdapter = new IndividualCompatibilityAdapter(individualData, userCropNames, this::onIndividualCellClick);
        rvCompatibility.setAdapter(individualAdapter);

        setupScrollSync();
    }

    private void loadUserCrops() {
        RetrofitClient.getApiService().getUserCrops(currentUserId).enqueue(new Callback<List<UserCrop>>() {
            @Override
            public void onResponse(Call<List<UserCrop>> call, Response<List<UserCrop>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    userCrops = response.body();
                    loadIndividualData();
                } else {
                    Toast.makeText(CompatibilityActivity.this, "У вас нет добавленных растений", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<UserCrop>> call, Throwable t) {
                Toast.makeText(CompatibilityActivity.this, "Ошибка загрузки растений", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadIndividualData() {
        RetrofitClient.getApiService().getIndividualCompatibilityMatrix(currentUserId)
                .enqueue(new Callback<List<IndividualCompatibilityDTO>>() {
                    @Override
                    public void onResponse(Call<List<IndividualCompatibilityDTO>> call, Response<List<IndividualCompatibilityDTO>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            individualData = response.body();
                        } else {
                            individualData = new ArrayList<>();
                        }
                        setupIndividualTable();
                    }

                    @Override
                    public void onFailure(Call<List<IndividualCompatibilityDTO>> call, Throwable t) {
                        Toast.makeText(CompatibilityActivity.this, "Ошибка загрузки связей", Toast.LENGTH_SHORT).show();
                        setupIndividualTable();
                    }
                });
    }

    private void onIndividualCellClick(int crop1Id, int crop2Id, int currentStatus, int position) {
        if (crop1Id == crop2Id) {
            Toast.makeText(this, "Нельзя менять совместимость с самим собой", Toast.LENGTH_SHORT).show();
            return;
        }

        String crop1Name = individualAdapter.getCropNameByPosition(position / userCrops.size());
        String crop2Name = individualAdapter.getCropNameByPosition(position % userCrops.size());

        String[] statusOptions = {"Нет данных", "Конфликт", "Нейтральная", "Хорошая"};

        int checkedIndex;
        switch (currentStatus) {
            case 2: checkedIndex = 1; break;
            case 3: checkedIndex = 2; break;
            case 4: checkedIndex = 3; break;
            default: checkedIndex = 0; break;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(crop1Name + " + " + crop2Name);

        builder.setSingleChoiceItems(statusOptions, checkedIndex, (dialog, which) -> {
            int newStatus;
            switch (which) {
                case 1: newStatus = 2; break;
                case 2: newStatus = 3; break;
                case 3: newStatus = 4; break;
                default: newStatus = 1; break;
            }

            individualAdapter.updateCell(position, newStatus);

            IndividualCompatibilityDTO dto = new IndividualCompatibilityDTO(
                    crop1Id, crop2Id, newStatus, currentUserId
            );

            RetrofitClient.getApiService().updateIndividualCompatibility(dto)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (!response.isSuccessful()) {
                                individualAdapter.updateCell(position, currentStatus);
                                Toast.makeText(CompatibilityActivity.this, "Ошибка", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            individualAdapter.updateCell(position, currentStatus);
                            Toast.makeText(CompatibilityActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                        }
                    });
            dialog.dismiss();
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void setupScrollSync() {
        dataHorizontalScroll.setOnScrollChangeListener((v, x, y, oldX, oldY) -> {
            headerScroll.scrollTo(x, 0);
        });

        rvCompatibility.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                sideScroll.scrollBy(0, dy);
            }
        });
    }
}