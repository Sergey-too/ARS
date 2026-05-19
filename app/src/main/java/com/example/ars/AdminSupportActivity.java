package com.example.ars;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
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

public class AdminSupportActivity extends AppCompatActivity {
    private RecyclerView rv;
    private SupportAdapter adapter;
    private List<SupportRequest> allRequests = new ArrayList<>();
    private ApiService apiService;
    private int adminId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_support);

        apiService = RetrofitClient.getApiService();
        SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(this);

        if (prefsHelper.getUser() != null) {
            adminId = prefsHelper.getUser().getId();
        }

        rv = findViewById(R.id.rvAdminSupportRequests);
        rv.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadAllRequests();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllRequests();
    }

    private void loadAllRequests() {
        apiService.getAllRequests().enqueue(new Callback<List<SupportRequest>>() {
            @Override
            public void onResponse(Call<List<SupportRequest>> call, Response<List<SupportRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allRequests = response.body();

                    adapter = new SupportAdapter(allRequests, new SupportAdapter.OnRequestClickListener() {
                        @Override
                        public void onEdit(SupportRequest req) {

                            Intent intent = new Intent(AdminSupportActivity.this, ChatActivity.class);
                            intent.putExtra("REQUEST_ID", req.getId());
                            intent.putExtra("USER_ID", adminId);
                            intent.putExtra("SUBJECT", req.getSubject());
                            intent.putExtra("CONTENT", req.getContent());
                            startActivity(intent);
                        }

                        @Override
                        public void onDelete(Integer id) {

                            openStatusChangingDialog(id);
                        }
                    });
                    rv.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<List<SupportRequest>> call, Throwable t) {
                Log.e("API_ERROR", t.getMessage());
            }
        });
    }

    private void openStatusChangingDialog(Integer requestId) {
        String[] statuses = {"Новый", "В работе", "Закрыт"};

        new AlertDialog.Builder(this)
                .setTitle("Изменить статус тикета")
                .setItems(statuses, (dialog, which) -> {
                    int selectedStatusId = 1;
                    if (which == 1) selectedStatusId = 3;
                    if (which == 2) selectedStatusId = 4;

                    apiService.updateRequestStatus(requestId, selectedStatusId).enqueue(new Callback<SupportRequest>() {
                        @Override
                        public void onResponse(Call<SupportRequest> call, Response<SupportRequest> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(AdminSupportActivity.this, "Статус изменен", Toast.LENGTH_SHORT).show();
                                loadAllRequests();
                            }
                        }

                        @Override
                        public void onFailure(Call<SupportRequest> call, Throwable t) {
                            Toast.makeText(AdminSupportActivity.this, "Не удалось изменить статус", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}