package com.example.ars;

import android.app.DatePickerDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.adapters.HistoryAdapter;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.History;
import com.example.ars.utils.SharedPreferencesHelper;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryActivity extends AppCompatActivity {

    private ApiService apiService;
    private SharedPreferencesHelper prefsHelper;
    private HistoryAdapter adapter;
    private ProgressBar progressBar;
    private ExtendedFloatingActionButton fabExportPdf;

    private List<History> allHistory = new ArrayList<>();
    private List<History> filteredList = new ArrayList<>();

    private Long dateStartMs = null;
    private Long dateEndMs = null;
    private String selectedAction = "Все работы";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        apiService = RetrofitClient.getApiService();
        prefsHelper = new SharedPreferencesHelper(this);
        progressBar = findViewById(R.id.progressBar);
        fabExportPdf = findViewById(R.id.fabExportPdf);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        fabExportPdf.setOnClickListener(v -> generatePdf());

        setupRecyclerView();
        setupFilters();
        loadHistory();
    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rvHistory);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(filteredList);
        rv.setAdapter(adapter);
    }

    private void setupFilters() {
        findViewById(R.id.btnDateFrom).setOnClickListener(v -> showDatePicker(true));
        findViewById(R.id.btnDateTo).setOnClickListener(v -> showDatePicker(false));

        AutoCompleteTextView actionSpinner = findViewById(R.id.actvActionFilter);
        String[] actions = {"Все работы", "Посадка", "Полив", "Удобрение", "Сбор урожая", "Рыхление", "Защита"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, actions);
        actionSpinner.setAdapter(spinnerAdapter);
        actionSpinner.setOnItemClickListener((parent, view, position, id) -> {
            selectedAction = actions[position];
            applyFilters();
        });
    }

    private void loadHistory() {
        if (prefsHelper.getUser() == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        int userId = prefsHelper.getUser().getId();

        apiService.getHistory(userId).enqueue(new Callback<List<History>>() {
            @Override
            public void onResponse(Call<List<History>> call, Response<List<History>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    allHistory = response.body();
                    applyFilters();
                } else {
                    Toast.makeText(HistoryActivity.this, "Нет данных", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<History>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(HistoryActivity.this, "Ошибка: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        filteredList.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (History item : allHistory) {
            boolean matchesDate = true;
            boolean matchesAction = selectedAction.equals("Все работы") ||
                    (item.getActionName() != null && item.getActionName().equals(selectedAction));

            try {
                String dateStr = item.getDoneAt();
                if (dateStr != null) {
                    dateStr = dateStr.split("[T ]")[0];
                }
                long itemTime = sdf.parse(dateStr).getTime();
                if (dateStartMs != null && itemTime < dateStartMs) matchesDate = false;
                if (dateEndMs != null && itemTime > dateEndMs) matchesDate = false;
            } catch (Exception ignored) {
                matchesDate = false;
            }

            if (matchesDate && matchesAction) {
                filteredList.add(item);
            }
        }
        adapter.updateList(filteredList);
    }

    private void showDatePicker(boolean isStart) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day);
            if (isStart) {
                dateStartMs = selected.getTimeInMillis();
                Button btn = findViewById(R.id.btnDateFrom);
                btn.setText("С: " + day + "." + (month + 1) + "." + year);
            } else {
                dateEndMs = selected.getTimeInMillis();
                Button btn = findViewById(R.id.btnDateTo);
                btn.setText("По: " + day + "." + (month + 1) + "." + year);
            }
            applyFilters();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void generatePdf() {
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "Нет данных для отчета", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        Paint borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1f);
        borderPaint.setColor(Color.GRAY);

        paint.setTextSize(18f);
        paint.setFakeBoldText(true);
        paint.setColor(Color.parseColor("#2E7D32"));
        canvas.drawText("ОТЧЕТ ПО РАБОТАМ", 50, 40, paint);

        paint.setTextSize(10f);
        paint.setFakeBoldText(false);
        paint.setColor(Color.BLACK);
        canvas.drawText("Сформирован: " + new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date()), 50, 65, paint);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#2E7D32"));
        linePaint.setStrokeWidth(2f);
        canvas.drawLine(25, 75, 570, 75, linePaint);

        int startX = 25;
        int startY = 95;
        int rowHeight = 28;
        int[] colWidths = {70, 120, 100, 100, 80};
        String[] headers = {"Дата", "Растение", "Сорт", "Участок", "Действие"};

        Paint headerBg = new Paint();
        headerBg.setColor(Color.parseColor("#E8F5E9"));

        paint.setFakeBoldText(true);
        paint.setTextSize(10f);
        paint.setColor(Color.BLACK);

        int currentX = startX;
        for (int i = 0; i < headers.length; i++) {
            canvas.drawRect(currentX, startY, currentX + colWidths[i], startY + rowHeight, headerBg);
            canvas.drawRect(currentX, startY, currentX + colWidths[i], startY + rowHeight, borderPaint);
            float textWidth = paint.measureText(headers[i]);
            canvas.drawText(headers[i], currentX + (colWidths[i] - textWidth) / 2, startY + 19, paint);
            currentX += colWidths[i];
        }

        paint.setFakeBoldText(false);
        paint.setTextSize(9f);
        int y = startY + rowHeight;

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        for (History item : filteredList) {
            if (y + rowHeight > 800) {
                document.finishPage(page);
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = startY;

                currentX = startX;
                paint.setFakeBoldText(true);
                for (int i = 0; i < headers.length; i++) {
                    canvas.drawRect(currentX, startY, currentX + colWidths[i], startY + rowHeight, headerBg);
                    canvas.drawRect(currentX, startY, currentX + colWidths[i], startY + rowHeight, borderPaint);
                    float textWidth = paint.measureText(headers[i]);
                    canvas.drawText(headers[i], currentX + (colWidths[i] - textWidth) / 2, startY + 19, paint);
                    currentX += colWidths[i];
                }
                paint.setFakeBoldText(false);
            }

            String dateStr = item.getDoneAt() != null ? item.getDoneAt() : "----";
            String formattedDate = "----";
            try {
                Date date = inputFormat.parse(dateStr);
                formattedDate = outputFormat.format(date);
            } catch (Exception e) {
                try {
                    SimpleDateFormat altInputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date date = altInputFormat.parse(dateStr);
                    formattedDate = outputFormat.format(date);
                } catch (Exception ex) {
                    formattedDate = dateStr;
                }
            }

            String crop = item.getCropName() != null ? item.getCropName() : "---";
            String variety = item.getVariety() != null ? item.getVariety() : "---";
            String area = item.getAreaName() != null ? item.getAreaName() : "---";
            String action = item.getActionName() != null ? item.getActionName() : "---";

            String[] rowData = {formattedDate, crop, variety, area, action};

            currentX = startX;
            for (int i = 0; i < rowData.length; i++) {
                canvas.drawRect(currentX, y, currentX + colWidths[i], y + rowHeight, borderPaint);
                String text = rowData[i];
                if (paint.measureText(text) > colWidths[i] - 8) {
                    while (paint.measureText(text + "...") > colWidths[i] - 8 && text.length() > 3) {
                        text = text.substring(0, text.length() - 1);
                    }
                    text = text + "...";
                }
                canvas.drawText(text, currentX + 4, y + 18, paint);
                currentX += colWidths[i];
            }
            y += rowHeight;
        }

        canvas.drawLine(25, y + 5, 570, y + 5, linePaint);
        paint.setTextSize(10f);
        paint.setFakeBoldText(true);
        canvas.drawText("Всего записей: " + filteredList.size(), 50, y + 25, paint);

        document.finishPage(page);

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String fileName = "HistoryReport_" + sdf.format(new Date()) + ".pdf";

            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, fileName);

            document.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF сохранен в Загрузки: " + fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        document.close();
    }
}