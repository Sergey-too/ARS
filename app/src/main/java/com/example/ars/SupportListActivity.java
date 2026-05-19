package com.example.ars;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.adapters.SupportAdapter;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.SupportRequest;
import com.example.ars.utils.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SupportListActivity extends AppCompatActivity {
    private RecyclerView rv;
    private SupportAdapter adapter;
    private List<SupportRequest> requestList = new ArrayList<>();

    private ApiService apiService;
    private SharedPreferencesHelper prefsHelper;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_list);

        apiService = RetrofitClient.getApiService();
        prefsHelper = new SharedPreferencesHelper(this);

        if (prefsHelper.getUser() != null) {
            currentUserId = prefsHelper.getUser().getId();
        } else {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rv = findViewById(R.id.rvSupportRequests);
        rv.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.fabAddRequest).setOnClickListener(v -> showSupportDialog(null));

        loadRequests();
    }

    private void loadRequests() {
        apiService.getUserRequests(currentUserId).enqueue(new Callback<List<SupportRequest>>() {
            @Override
            public void onResponse(Call<List<SupportRequest>> call, Response<List<SupportRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    requestList = response.body();
                    adapter = new SupportAdapter(requestList, new SupportAdapter.OnRequestClickListener() {
                        @Override public void onEdit(SupportRequest req) { showSupportDialog(req); }
                        @Override public void onDelete(Integer id) { confirmDeletion(id); }
                    });
                    rv.setAdapter(adapter);
                } else {
                    Log.e("API_ERROR", "Ошибка: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<SupportRequest>> call, Throwable t) {
                Log.e("API_ERROR", "Fail: " + t.getMessage());
                Toast.makeText(SupportListActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSupportDialog(SupportRequest existingRequest) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_support, null);

        EditText etSubject = view.findViewById(R.id.etSubject);
        EditText etContent = view.findViewById(R.id.etContent);
        Button btnSave = view.findViewById(R.id.btnSave);

        if (existingRequest != null) {
            etSubject.setText(existingRequest.getSubject());
            etContent.setText(existingRequest.getContent());
        }

        AlertDialog dialog = builder.setView(view).create();

        btnSave.setOnClickListener(v -> {
            String sub = etSubject.getText().toString().trim();
            String body = etContent.getText().toString().trim();

            if (sub.isEmpty() || body.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            if (existingRequest == null) {
                SupportRequest newReq = new SupportRequest(currentUserId, sub, body);
                sendNewRequest(newReq, dialog);
            }
            else {
                updateRequest(existingRequest.getId(), sub, body, dialog);
            }
        });

        dialog.show();
    }

    private void sendNewRequest(SupportRequest req, AlertDialog dialog) {
        apiService.createSupportRequest(req).enqueue(new Callback<SupportRequest>() {
            @Override
            public void onResponse(Call<SupportRequest> call, Response<SupportRequest> response) {
                if (response.isSuccessful()) {
                    dialog.dismiss();
                    loadRequests();
                } else {
                    Toast.makeText(SupportListActivity.this, "Ошибка при сохранении", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SupportRequest> call, Throwable t) {
                Toast.makeText(SupportListActivity.this, "Ошибка соединения", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRequest(Integer id, String sub, String body, AlertDialog dialog) {
        SupportRequest updated = new SupportRequest(currentUserId, sub, body);
        apiService.updateSupportRequest(id, updated).enqueue(new Callback<SupportRequest>() {
            @Override
            public void onResponse(Call<SupportRequest> call, Response<SupportRequest> response) {
                if (response.isSuccessful()) {
                    dialog.dismiss();
                    loadRequests();
                }
            }

            @Override
            public void onFailure(Call<SupportRequest> call, Throwable t) {
                Toast.makeText(SupportListActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeletion(Integer id) {
        new AlertDialog.Builder(this)
                .setTitle("Удаление")
                .setMessage("Вы уверены, что хотите удалить запрос?")
                .setPositiveButton("Да", (d, w) -> {
                    apiService.deleteSupportRequest(id).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                loadRequests();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(SupportListActivity.this, "Ошибка при удалении", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Нет", null)
                .show();
    }
}