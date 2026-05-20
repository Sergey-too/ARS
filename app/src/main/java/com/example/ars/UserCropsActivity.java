package com.example.ars;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ars.adapters.UserCropAdapter;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.IndividualUserCrop;
import com.example.ars.utils.SharedPreferencesHelper;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserCropsActivity extends AppCompatActivity {

    private ApiService apiService;
    private SharedPreferencesHelper prefsHelper;
    private UserCropAdapter adapter;
    private List<IndividualUserCrop> originalList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_crops_list);

        apiService = RetrofitClient.getApiService();
        prefsHelper = new SharedPreferencesHelper(this);

        RecyclerView rv = findViewById(R.id.rvUserCrops);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new UserCropAdapter(new ArrayList<>(), crop -> {
            Intent intent = new Intent(this, EditPlantActivityUser.class);
            intent.putExtra("CROP_ID", crop.getId());
            startActivity(intent);
        });
        rv.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAddUserCrop).setOnClickListener(v -> {
            startActivity(new Intent(this, AddPlantActivityUser.class));
        });

        setupSearch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyCrops();
    }

    private void loadMyCrops() {
        int userId = prefsHelper.getUser().getId();

        apiService.getIndividualUserCrops(userId).enqueue(new Callback<List<IndividualUserCrop>>() {
            @Override
            public void onResponse(Call<List<IndividualUserCrop>> call, Response<List<IndividualUserCrop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    originalList = response.body();
                    adapter.updateList(originalList);
                }
            }

            @Override
            public void onFailure(Call<List<IndividualUserCrop>> call, Throwable t) {
                Toast.makeText(UserCropsActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearch() {
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filter(String query) {
        List<IndividualUserCrop> filtered = new ArrayList<>();
        for (IndividualUserCrop crop : originalList) {
            if (crop.getName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(crop);
            }
        }
        adapter.updateList(filtered);
    }
}