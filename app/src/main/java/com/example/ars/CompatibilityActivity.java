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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.adapters.CompatibilityAdapter;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient; // Используем твой класс
import com.example.ars.models.CompatibilityDTO;

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

        loadData();
    }

    private void loadData() {
        RetrofitClient.getApiService().getCompatibilityMatrix().enqueue(new Callback<List<CompatibilityDTO>>() {
            @Override
            public void onResponse(Call<List<CompatibilityDTO>> call, Response<List<CompatibilityDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    setupTable(response.body());
                }
            }
            @Override public void onFailure(Call<List<CompatibilityDTO>> call, Throwable t) {}
        });
    }

    private void setupTable(List<CompatibilityDTO> data) {
        // 1. Получаем список уникальных названий для осей
        List<String> crops = data.stream()
                .map(CompatibilityDTO::getCrop1)
                .distinct()
                .sorted() // Сортируем, чтобы оси совпадали с сеткой
                .collect(Collectors.toList());

        int n = crops.size();

        // 2. Очищаем старые заголовки (если есть)
        containerTopNames.removeAllViews();
        containerLeftNames.removeAllViews();

        // 3. Создаем заголовки
        int cellSize = (int) (51 * getResources().getDisplayMetrics().density); // 50dp + margin

        for (String name : crops) {
            // Верхние названия
            TextView tvTop = new TextView(this);
            tvTop.setText(name);
            tvTop.setGravity(Gravity.CENTER);
            tvTop.setLayoutParams(new LinearLayout.LayoutParams(cellSize, 240)); // 240 - высота шапки
            tvTop.setRotation(-90);
            containerTopNames.addView(tvTop);

            // Левые названия
            TextView tvLeft = new TextView(this);
            tvLeft.setText(name);
            tvLeft.setGravity(Gravity.CENTER_VERTICAL);
            tvLeft.setPadding(10, 0, 0, 0);
            tvLeft.setLayoutParams(new LinearLayout.LayoutParams(240, cellSize)); // 240 - ширина боковины
            containerLeftNames.addView(tvLeft);
        }

        // 4. Настраиваем сетку
        rvCompatibility.setLayoutManager(new GridLayoutManager(this, n));
        rvCompatibility.setAdapter(new CompatibilityAdapter(data));

        // 5. Синхронизируем скролл (чтобы шапки ехали вместе с таблицей)
        dataHorizontalScroll.setOnScrollChangeListener((v, x, y, oldX, oldY) -> headerScroll.scrollTo(x, 0));
        rvCompatibility.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                sideScroll.scrollBy(0, dy);
            }
        });
    }

    private TextView createLabel(String text, boolean isTop) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(10);
        tv.setPadding(4, 4, 4, 4);
        tv.setTextColor(Color.BLACK);
        // Размер должен совпадать с размером ячейки (50dp) + margin
        int size = (int) (51 * getResources().getDisplayMetrics().density);
        if (isTop) {
            tv.setLayoutParams(new LinearLayout.LayoutParams(size, 80 * 3)); // 80dp высота шапки
            tv.setRotation(-90); // Поворачиваем текст как на фото
        } else {
            tv.setLayoutParams(new LinearLayout.LayoutParams(80 * 3, size)); // 80dp ширина боковины
        }
        return tv;
    }
}