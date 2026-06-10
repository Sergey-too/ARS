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
import com.example.ars.adapters.GardenAdapter;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.Area;
import com.example.ars.models.Garden;
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

public class GardensActivity extends AppCompatActivity {

    private ApiService apiService;
    private SharedPreferencesHelper prefsHelper;
    private GardenAdapter adapter;
    private List<Garden> currentUserGardens = new ArrayList<>();
    private List<Garden> originalGardens = new ArrayList<>();
    private List<Area> allUserAreas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gardens_list);

        apiService = RetrofitClient.getApiService();
        prefsHelper = new SharedPreferencesHelper(this);

        RecyclerView rv = findViewById(R.id.rvGardens);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new GardenAdapter(new ArrayList<>(), this::showGardenDialog);
        rv.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAddGarden).setOnClickListener(v -> showGardenDialog(null));

        loadUserAreas();
        loadUserGardens();
        setupSearch();
    }

    private void loadUserAreas() {
        apiService.getUserAreas(prefsHelper.getUser().getId()).enqueue(new Callback<List<Area>>() {
            @Override
            public void onResponse(Call<List<Area>> call, Response<List<Area>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allUserAreas = response.body();
                    Log.d("GardensActivity", "Загружено участков: " + allUserAreas.size());
                }
            }

            @Override
            public void onFailure(Call<List<Area>> call, Throwable t) {
                Log.e("GardensActivity", "Ошибка загрузки участков", t);
            }
        });
    }

    private void loadUserGardens() {
        apiService.getUserGardens(prefsHelper.getUser().getId()).enqueue(new Callback<List<Garden>>() {
            @Override
            public void onResponse(Call<List<Garden>> call, Response<List<Garden>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUserGardens = response.body();
                    originalGardens = new ArrayList<>(currentUserGardens);
                    adapter.updateList(currentUserGardens);
                } else {
                    Toast.makeText(GardensActivity.this, "Нет огородов", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Garden>> call, Throwable t) {
                Toast.makeText(GardensActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showGardenDialog(Garden garden) {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        View view = getLayoutInflater().inflate(R.layout.dialog_garden_form, null);

        TextInputLayout tilName = view.findViewById(R.id.tilDialogGardenName);
        EditText etName = view.findViewById(R.id.etDialogGardenName);
        TextInputLayout tilArea = view.findViewById(R.id.tilDialogArea);
        AutoCompleteTextView actvArea = view.findViewById(R.id.actvDialogArea);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveGarden);
        MaterialButton btnDelete = view.findViewById(R.id.btnDeleteGarden);

        ArrayAdapter<Area> areasAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, allUserAreas);
        actvArea.setAdapter(areasAdapter);

        final Area[] selectedAreaHolder = {null};

        actvArea.setOnItemClickListener((parent, view1, position, id) -> {
            selectedAreaHolder[0] = (Area) parent.getItemAtPosition(position);
            tilArea.setError(null);
        });

        if (garden != null) {
            etName.setText(garden.getName());
            if (garden.getAreas() != null && !garden.getAreas().isEmpty()) {
                Area gardenArea = garden.getAreas().get(0);
                selectedAreaHolder[0] = gardenArea;
                actvArea.setText(gardenArea.getName(), false);
            }
            btnDelete.setVisibility(View.VISIBLE);

            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Удаление")
                        .setMessage("Вы точно хотите удалить огород \"" + garden.getName() + "\"?")
                        .setPositiveButton("Удалить", (d, w) -> deleteGarden(garden.getId(), dialog))
                        .setNegativeButton("Отмена", null)
                        .show();
            });
        }

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            tilName.setError(null);

            if (name.isEmpty()) {
                tilName.setError("Введите название огорода");
                return;
            }

            if (selectedAreaHolder[0] == null) {
                tilArea.setError("Выберите участок");
                return;
            }

            for (Garden existingGarden : currentUserGardens) {
                if (existingGarden.getName().equalsIgnoreCase(name)) {
                    if (garden == null || !garden.getId().equals(existingGarden.getId())) {
                        tilName.setError("Огород с таким именем уже существует!");
                        return;
                    }
                }
            }

            if (garden == null) {
                createGarden(name, selectedAreaHolder[0], dialog);
            } else {
                updateGarden(garden.getId(), name, selectedAreaHolder[0], dialog);
            }
        });

        dialog.setView(view);
        dialog.show();
    }

    private void createGarden(String name, Area area, AlertDialog d) {
        Garden garden = new Garden();
        garden.setName(name);
        garden.setUserId(prefsHelper.getUser().getId());

        apiService.createGarden(garden).enqueue(new Callback<Garden>() {
            @Override
            public void onResponse(Call<Garden> call, Response<Garden> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Garden createdGarden = response.body();
                    addAreaToGarden(createdGarden.getId(), area, d);
                } else {
                    Toast.makeText(GardensActivity.this, "Ошибка создания", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Garden> call, Throwable t) {
                Toast.makeText(GardensActivity.this, "Ошибка соединения", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addAreaToGarden(int gardenId, Area area, AlertDialog d) {
        Map<String, Integer> request = new HashMap<>();
        request.put("areaId", area.getId());

        apiService.addAreaToGarden(gardenId, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(GardensActivity.this, "Огород создан!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(GardensActivity.this, "Ошибка при добавлении участка", Toast.LENGTH_SHORT).show();
                }
                d.dismiss();
                loadUserGardens();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("GardensActivity", "Ошибка добавления участка", t);
                d.dismiss();
                loadUserGardens();
            }
        });
    }

    private void updateGarden(Integer gardenId, String newName, Area area, AlertDialog dialog) {
        Garden garden = new Garden();
        garden.setName(newName);
        garden.setUserId(prefsHelper.getUser().getId());

        apiService.updateGarden(gardenId, garden).enqueue(new Callback<Garden>() {
            @Override
            public void onResponse(Call<Garden> call, Response<Garden> response) {
                if (response.isSuccessful()) {
                    updateGardenAreas(gardenId, area, dialog);
                } else {
                    Toast.makeText(GardensActivity.this, "Ошибка обновления", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Garden> call, Throwable t) {
                Toast.makeText(GardensActivity.this, "Ошибка соединения", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateGardenAreas(int gardenId, Area newArea, AlertDialog dialog) {
        for (Area area : allUserAreas) {
            apiService.removeAreaFromGarden(gardenId, area.getId()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                }
            });
        }

        Map<String, Integer> request = new HashMap<>();
        request.put("areaId", newArea.getId());

        apiService.addAreaToGarden(gardenId, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Toast.makeText(GardensActivity.this, "Огород обновлен!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadUserGardens();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("GardensActivity", "Ошибка добавления участка", t);
                dialog.dismiss();
                loadUserGardens();
            }
        });
    }

    private void deleteGarden(Integer gardenId, AlertDialog dialog) {
        apiService.deleteGarden(gardenId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(GardensActivity.this, "Огород удален", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadUserGardens();
                } else {
                    Toast.makeText(GardensActivity.this, "Нельзя удалить огород", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(GardensActivity.this, "Ошибка при удалении", Toast.LENGTH_SHORT).show();
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
                adapter.updateList(originalGardens);
            });
        }
    }

    private void filter(String text) {
        List<Garden> filteredList = new ArrayList<>();

        for (Garden garden : originalGardens) {
            String name = garden.getName().toLowerCase();
            if (name.contains(text.toLowerCase())) {
                filteredList.add(garden);
            }
        }

        adapter.updateList(filteredList);
    }
}