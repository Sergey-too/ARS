package com.example.ars;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.adapters.UserAdapter;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.User;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UsersListActivityAdmin extends AppCompatActivity {

    private ApiService apiService;
    private UserAdapter adapter;
    private List<User> allUsers = new ArrayList<>();
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_users);

        apiService = RetrofitClient.getApiService();
        progressBar = findViewById(R.id.progressBar);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        RecyclerView rvUsers = findViewById(R.id.rvUsers);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));

        adapter = new UserAdapter(new ArrayList<>(), new UserAdapter.OnUserActionListener() {
            @Override
            public void onAdminToggle(User user) {
                toggleAdminStatus(user);
            }

            @Override
            public void onBanToggle(User user) {
                toggleBanStatus(user);
            }
        });
        rvUsers.setAdapter(adapter);

        setupSearch();
        loadUsers();
    }

    private void setupSearch() {
        TextInputEditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                filter(s.toString());
            }
        });
    }

    private void filter(String text) {
        List<User> filteredList = new ArrayList<>();
        for (User user : allUsers) {
            if (user.getLogin().toLowerCase().contains(text.toLowerCase()) ||
                    user.getEmail().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(user);
            }
        }
        adapter.updateData(filteredList);
    }

    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getAllUsers().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    allUsers = response.body();
                    adapter.updateData(allUsers);
                }
            }
            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(UsersListActivityAdmin.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleAdminStatus(User user) {
        apiService.toggleAdmin(user.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    loadUsers();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void toggleBanStatus(User user) {
        apiService.toggleBan(user.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    loadUsers();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }
}