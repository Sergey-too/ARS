package com.example.ars;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.adapters.WeatherAdminAdapter;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.WeatherData;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherAdminActivity extends AppCompatActivity {

    private RecyclerView rvWeather;
    private ProgressBar progressBar;
    private View llEmpty;
    private TextView tvCount;
    private TextInputEditText etDateFrom, etDateTo;
    private MaterialButton btnApplyFilter, btnSortNewest, btnSortOldest;
    private MaterialButton btnRefresh;  // вместо FAB используем обычную кнопку

    private WeatherAdminAdapter adapter;
    private ApiService apiService;
    private List<WeatherData> allWeather = new ArrayList<>();
    private List<WeatherData> filteredWeather = new ArrayList<>();

    private String sortOrder = "desc";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_admin);

        apiService = RetrofitClient.getApiService();
        initViews();
        setupToolbar();
        setupAdapter();
        setupListeners();
        loadWeatherData();
    }

    private void initViews() {
        rvWeather = findViewById(R.id.rvWeatherList);
        progressBar = findViewById(R.id.progressBar);
        llEmpty = findViewById(R.id.llEmpty);
        tvCount = findViewById(R.id.tvCount);
        etDateFrom = findViewById(R.id.etDateFrom);
        etDateTo = findViewById(R.id.etDateTo);
        btnApplyFilter = findViewById(R.id.btnApplyFilter);
        btnSortNewest = findViewById(R.id.btnSortNewest);
        btnSortOldest = findViewById(R.id.btnSortOldest);
        btnRefresh = findViewById(R.id.btnRefresh);  // кнопка обновления
    }

    private void setupToolbar() {
        // Кнопка назад
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupAdapter() {
        adapter = new WeatherAdminAdapter();
        adapter.setOnDeleteClickListener((weather, position) -> confirmDelete(weather, position));
        rvWeather.setLayoutManager(new LinearLayoutManager(this));
        rvWeather.setAdapter(adapter);
    }

    private void setupListeners() {
        btnApplyFilter.setOnClickListener(v -> applyFilterAndSort());

        btnSortNewest.setOnClickListener(v -> {
            sortOrder = "desc";
            applyFilterAndSort();
        });

        btnSortOldest.setOnClickListener(v -> {
            sortOrder = "asc";
            applyFilterAndSort();
        });

        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> loadWeatherData());
        }
    }

    private void loadWeatherData() {
        progressBar.setVisibility(View.VISIBLE);
        rvWeather.setVisibility(View.GONE);
        llEmpty.setVisibility(View.GONE);

        apiService.getAllWeather().enqueue(new Callback<List<WeatherData>>() {
            @Override
            public void onResponse(Call<List<WeatherData>> call, Response<List<WeatherData>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    allWeather = response.body();
                    applyFilterAndSort();
                } else {
                    allWeather.clear();
                    filteredWeather.clear();
                    adapter.setData(filteredWeather);
                    rvWeather.setVisibility(View.GONE);
                    llEmpty.setVisibility(View.VISIBLE);
                    tvCount.setText("Найдено: 0 записей");
                    Toast.makeText(WeatherAdminActivity.this, "Нет данных о погоде", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<WeatherData>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(WeatherAdminActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                llEmpty.setVisibility(View.VISIBLE);
                rvWeather.setVisibility(View.GONE);
            }
        });
    }

    private void applyFilterAndSort() {
        String dateFrom = etDateFrom.getText().toString().trim();
        String dateTo = etDateTo.getText().toString().trim();

        filteredWeather.clear();

        for (WeatherData w : allWeather) {
            if (isWithinDateRange(w.getDate(), dateFrom, dateTo)) {
                filteredWeather.add(w);
            }
        }

        sortWeatherList(filteredWeather);
        adapter.setData(filteredWeather);
        tvCount.setText("Найдено: " + filteredWeather.size() + " записей");

        if (filteredWeather.isEmpty()) {
            llEmpty.setVisibility(View.VISIBLE);
            rvWeather.setVisibility(View.GONE);
        } else {
            llEmpty.setVisibility(View.GONE);
            rvWeather.setVisibility(View.VISIBLE);
        }
    }

    private boolean isWithinDateRange(String dateStr, String fromStr, String toStr) {
        if (TextUtils.isEmpty(fromStr) && TextUtils.isEmpty(toStr)) return true;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            java.util.Date date = sdf.parse(dateStr);

            if (!TextUtils.isEmpty(fromStr)) {
                java.util.Date from = sdf.parse(convertToIso(fromStr));
                if (date.before(from)) return false;
            }

            if (!TextUtils.isEmpty(toStr)) {
                java.util.Date to = sdf.parse(convertToIso(toStr));
                if (date.after(to)) return false;
            }
        } catch (Exception e) {
            return true;
        }
        return true;
    }

    private String convertToIso(String date) {
        if (date.contains(".")) {
            String[] parts = date.split("\\.");
            if (parts.length == 3) {
                return parts[2] + "-" + parts[1] + "-" + parts[0];
            }
        }
        return date;
    }

    private void sortWeatherList(List<WeatherData> list) {
        Collections.sort(list, new Comparator<WeatherData>() {
            @Override
            public int compare(WeatherData a, WeatherData b) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    java.util.Date dateA = sdf.parse(a.getDate());
                    java.util.Date dateB = sdf.parse(b.getDate());
                    if ("desc".equals(sortOrder)) {
                        return dateB.compareTo(dateA);
                    } else {
                        return dateA.compareTo(dateB);
                    }
                } catch (Exception e) {
                    return 0;
                }
            }
        });
    }

    private void confirmDelete(WeatherData weather, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Удаление записи")
                .setMessage("Удалить данные о погоде за " + formatDateForDialog(weather.getDate()) + "?")
                .setPositiveButton("Удалить", (d, w) -> deleteWeather(weather, position))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private String formatDateForDialog(String date) {
        if (date == null || date.isEmpty()) return "---";
        if (date.contains("-")) {
            String[] parts = date.split("-");
            if (parts.length == 3) {
                return parts[2] + "." + parts[1] + "." + parts[0];
            }
        }
        return date;
    }

    private void deleteWeather(WeatherData weather, int position) {
        progressBar.setVisibility(View.VISIBLE);

        apiService.deleteWeather(weather.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(WeatherAdminActivity.this, "Запись удалена", Toast.LENGTH_SHORT).show();
                    allWeather.remove(weather);
                    filteredWeather.remove(weather);
                    adapter.setData(filteredWeather);
                    tvCount.setText("Найдено: " + filteredWeather.size() + " записей");

                    if (filteredWeather.isEmpty()) {
                        llEmpty.setVisibility(View.VISIBLE);
                        rvWeather.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(WeatherAdminActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(WeatherAdminActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }
}