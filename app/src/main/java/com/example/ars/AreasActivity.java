package com.example.ars;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.R;
import com.example.ars.adapters.AreaAdapter;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.Area;
import com.example.ars.models.Region;
import com.example.ars.utils.SharedPreferencesHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AreasActivity extends AppCompatActivity {

    private ApiService apiService;
    private SharedPreferencesHelper prefsHelper;
    private AreaAdapter adapter;
    private List<Region> allRegions = new ArrayList<>();
    private List<Area> currentUserAreas = new ArrayList<>();
    private List<Area> originalAreas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_areas_list);

        apiService = RetrofitClient.getApiService();
        prefsHelper = new SharedPreferencesHelper(this);

        RecyclerView rv = findViewById(R.id.rvAreas);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AreaAdapter(new ArrayList<>(), this::showAreaDialog);
        rv.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAddArea).setOnClickListener(v -> showAreaDialog(null));

        loadRegions();
        loadUserAreas();
        setupSearch();
    }

    private void loadUserAreas() {
        apiService.getUserAreas(prefsHelper.getUser().getId()).enqueue(new Callback<List<Area>>() {
            @Override
            public void onResponse(Call<List<Area>> call, Response<List<Area>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUserAreas = response.body();
                    originalAreas = new ArrayList<>(currentUserAreas);
                    adapter.updateList(currentUserAreas);
                }
            }
            @Override
            public void onFailure(Call<List<Area>> call, Throwable t) {
                Toast.makeText(AreasActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAreaDialog(Area area) {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        View view = getLayoutInflater().inflate(R.layout.dialog_area_form, null);

        TextInputLayout tilName = view.findViewById(R.id.tilDialogAreaName);
        EditText etName = view.findViewById(R.id.etDialogAreaName);
        TextInputLayout tilRegion = view.findViewById(R.id.tilDialogRegion);
        AutoCompleteTextView actvRegion = view.findViewById(R.id.actvDialogRegion);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveArea);
        MaterialButton btnDelete = view.findViewById(R.id.btnDeleteArea);

        ArrayAdapter<Region> regAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, allRegions);
        actvRegion.setAdapter(regAdapter);

        final Integer[] selectedRegId = {null};
        actvRegion.setOnItemClickListener((p, v, pos, id) -> {
            selectedRegId[0] = Math.toIntExact(allRegions.get(pos).getId());
            tilRegion.setError(null);
        });

        if (area != null) {
            etName.setText(area.getName());
            if (area.getRegion() != null) {
                actvRegion.setText(area.getRegion().getName(), false);
            }
            selectedRegId[0] = area.getRegionId();
            btnDelete.setVisibility(View.VISIBLE);

            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Удаление")
                        .setMessage("Вы точно хотите удалить этот участок?")
                        .setPositiveButton("Удалить", (d, w) -> deleteArea(area.getId(), dialog))
                        .setNegativeButton("Отмена", null)
                        .show();
            });
        }

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            tilName.setError(null);
            if (name.isEmpty()) {
                tilName.setError("Введите название участка");
                return;
            }

            for (Area existingArea : currentUserAreas) {
                if (existingArea.getName().equalsIgnoreCase(name)) {
                    if (area == null || !area.getId().equals(existingArea.getId())) {
                        tilName.setError("Участок с таким именем уже существует!");
                        return;
                    }
                }
            }

            if (selectedRegId[0] == null) {
                Toast.makeText(this, "Выберите регион", Toast.LENGTH_SHORT).show();
                return;
            }

            if (area == null) createArea(name, selectedRegId[0], dialog);
            else updateArea(area.getId(), name, selectedRegId[0], dialog);
        });

        dialog.setView(view);
        dialog.show();
    }

    private void createArea(String name, Integer regId, AlertDialog d) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("regionId", regId);
        map.put("userId", prefsHelper.getUser().getId());

        apiService.addArea(map).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    d.dismiss();
                    loadUserAreas();
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(AreasActivity.this, "Ошибка соединения", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateArea(Integer areaId, String newName, Integer regId, AlertDialog dialog) {
        Map<String, Object> request = new HashMap<>();
        request.put("name", newName);
        request.put("regionId", regId);

        apiService.updateArea(areaId, request).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AreasActivity.this, "Обновлено", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadUserAreas();
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void deleteArea(Integer areaId, AlertDialog dialog) {
        apiService.deleteArea(areaId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AreasActivity.this, "Участок удален", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadUserAreas();
                } else {
                    Toast.makeText(AreasActivity.this, "Нельзя удалить используемый участок", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(AreasActivity.this, "Ошибка при удалении", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void loadRegions() {
        apiService.getRegions().enqueue(new Callback<List<Region>>() {
            @Override
            public void onResponse(Call<List<Region>> call, Response<List<Region>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allRegions = response.body();
                    Log.d("AreasActivity", "Загружено регионов: " + allRegions.size());
                } else {
                    Log.e("AreasActivity", "Ошибка загрузки регионов: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Region>> call, Throwable t) {
                Log.e("AreasActivity", "Ошибка сети при загрузке регионов", t);
            }
        });
    }

    private void setupSearch() {
        EditText etSearch = findViewById(R.id.etSearch);
        if (etSearch == null) return;

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        TextInputLayout searchLayout = findViewById(R.id.searchLayout);
        if (searchLayout != null) {
            searchLayout.setEndIconOnClickListener(v -> {
                etSearch.setText("");
                adapter.updateList(originalAreas);
            });
        }
    }
    private void filter(String text) {
        List<Area> filteredList = new ArrayList<>();

        for (Area area : originalAreas) {
            String name = area.getName().toLowerCase();
            String regionName = (area.getRegion() != null) ? area.getRegion().getName().toLowerCase() : "";

            if (name.contains(text.toLowerCase()) || regionName.contains(text.toLowerCase())) {
                filteredList.add(area);
            }
        }

        adapter.updateList(filteredList);
    }
}