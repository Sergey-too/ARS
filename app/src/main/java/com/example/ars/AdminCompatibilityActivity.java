package com.example.ars;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
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

import com.example.ars.adapters.AdminCompatibilityAdapter;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.CompatibilityDTO;

import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminCompatibilityActivity extends AppCompatActivity implements AdminCompatibilityAdapter.OnCellClickListener {

    private RecyclerView rvCompatibility;
    private LinearLayout containerTopNames, containerLeftNames;
    private HorizontalScrollView headerScroll, dataHorizontalScroll;
    private ScrollView sideScroll;

    private List<CompatibilityDTO> matrixData;
    private AdminCompatibilityAdapter adminAdapter;

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
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadData();
    }

    private void loadData() {
        RetrofitClient.getApiService().getCompatibilityMatrix().enqueue(new Callback<List<CompatibilityDTO>>() {
            @Override
            public void onResponse(Call<List<CompatibilityDTO>> call, Response<List<CompatibilityDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    matrixData = response.body();
                    setupTable(matrixData);
                }
            }
            @Override public void onFailure(Call<List<CompatibilityDTO>> call, Throwable t) {
                Toast.makeText(AdminCompatibilityActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupTable(List<CompatibilityDTO> data) {
        List<String> crops = data.stream()
                .map(CompatibilityDTO::getCrop1)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        int n = crops.size();

        containerTopNames.removeAllViews();
        containerLeftNames.removeAllViews();

        int cellSize = (int) (51 * getResources().getDisplayMetrics().density);

        for (String name : crops) {
            TextView tvTop = new TextView(this);
            tvTop.setText(name);
            tvTop.setGravity(Gravity.CENTER);
            tvTop.setLayoutParams(new LinearLayout.LayoutParams(cellSize, 240));
            tvTop.setRotation(-90);
            containerTopNames.addView(tvTop);

            TextView tvLeft = new TextView(this);
            tvLeft.setText(name);
            tvLeft.setGravity(Gravity.CENTER_VERTICAL);
            tvLeft.setPadding(10, 0, 0, 0);
            tvLeft.setLayoutParams(new LinearLayout.LayoutParams(240, cellSize));
            containerLeftNames.addView(tvLeft);
        }

        rvCompatibility.setLayoutManager(new GridLayoutManager(this, n));

        adminAdapter = new AdminCompatibilityAdapter(data, this);
        rvCompatibility.setAdapter(adminAdapter);

        dataHorizontalScroll.setOnScrollChangeListener((v, x, y, oldX, oldY) -> headerScroll.scrollTo(x, 0));
        rvCompatibility.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                sideScroll.scrollBy(0, dy);
            }
        });
    }

    @Override
    public void onCellClick(CompatibilityDTO item, int position) {
        if (item.getCrop1().equals(item.getCrop2())) {
            Toast.makeText(this, "Нельзя менять совместимость растения с самим собой", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] statusOptions = {"Нет данных", "Конфликт", "Нейтральная", "Хорошая"};

        int checkedIndex;
        switch (item.getStatus()) {
            case 2: checkedIndex = 1; break;
            case 3: checkedIndex = 2; break;
            case 4: checkedIndex = 3; break;
            default: checkedIndex = 0; break;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(item.getCrop1() + " + " + item.getCrop2());

        builder.setSingleChoiceItems(statusOptions, checkedIndex, (dialog, which) -> {
            int newStatus;
            switch (which) {
                case 1: newStatus = 2; break;
                case 2: newStatus = 3; break;
                case 3: newStatus = 4; break;
                default: newStatus = 1; break;
            }

            item.setStatus(newStatus);
            adminAdapter.notifyItemChanged(position);

            updateMirrorCellLocally(item.getCrop2(), item.getCrop1(), newStatus);

            sendNewStatusToServer(item);

            dialog.dismiss();
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void updateMirrorCellLocally(String crop1, String crop2, int newStatus) {
        if (matrixData == null) return;
        for (int i = 0; i < matrixData.size(); i++) {
            CompatibilityDTO dto = matrixData.get(i);
            if (dto.getCrop1().equals(crop1) && dto.getCrop2().equals(crop2)) {
                dto.setStatus(newStatus);
                adminAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void sendNewStatusToServer(CompatibilityDTO updatedItem) {
        RetrofitClient.getApiService().updateCompatibility(updatedItem).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminCompatibilityActivity.this, "Сохранено", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AdminCompatibilityActivity.this, "Ошибка при сохранении", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(AdminCompatibilityActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }
}