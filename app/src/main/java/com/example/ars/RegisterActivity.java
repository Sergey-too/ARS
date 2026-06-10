// RegisterActivity.java
package com.example.ars;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.AuthResponse;
import com.example.ars.models.User;
import com.example.ars.utils.SharedPreferencesHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";

    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword;
    private TextInputLayout tilName, tilEmail, tilPassword, tilConfirmPassword;
    private CheckBox cbTerms;
    private Button btnRegister;
    private SharedPreferencesHelper prefsHelper;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        prefsHelper = new SharedPreferencesHelper(this);
        RetrofitClient.initialize(prefsHelper);
        apiService = RetrofitClient.getApiService();

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        cbTerms = findViewById(R.id.cbTerms);
        btnRegister = findViewById(R.id.btnRegister);
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> attemptRegistration());

        findViewById(R.id.tvLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void attemptRegistration() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        boolean hasError = false;

        if (TextUtils.isEmpty(name)) {
            tilName.setError("Введите имя");
            hasError = true;
        } else if (name.length() < 2) {
            tilName.setError("Имя должно содержать минимум 2 символа");
            hasError = true;
        }

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Введите email");
            hasError = true;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Неверный формат email");
            hasError = true;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Введите пароль");
            hasError = true;
        } else if (password.length() < 6) {
            tilPassword.setError("Пароль должен содержать минимум 6 символов");
            hasError = true;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("Подтвердите пароль");
            hasError = true;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Пароли не совпадают");
            hasError = true;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Необходимо принять условия использования", Toast.LENGTH_SHORT).show();
            hasError = true;
        }

        if (!hasError) {
            performRegistration(name, email, password);
        }
    }

    private void performRegistration(String name, String email, String password) {
        btnRegister.setEnabled(false);
        btnRegister.setText("Регистрация...");

        String login = email.split("@")[0];
        login = login.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

        if (login.isEmpty()) {
            login = "user" + System.currentTimeMillis();
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setLogin(login);
        user.setPassword(password);

        apiService.register(user).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                btnRegister.setEnabled(true);
                btnRegister.setText("Зарегистрироваться");

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();

                    if (authResponse.isSuccess()) {
                        prefsHelper.saveToken(authResponse.getToken());
                        prefsHelper.saveUser(authResponse.getUser());
                        prefsHelper.setLoggedIn(true);

                        Toast.makeText(RegisterActivity.this,
                                "Регистрация успешна!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(RegisterActivity.this, PlantsActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        String errorMsg = authResponse.getError() != null ?
                                authResponse.getError() : "Ошибка регистрации";
                        Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    String errorMsg = "Ошибка сервера: " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing error body", e);
                        }
                    }
                    Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                btnRegister.setEnabled(true);
                btnRegister.setText("Зарегистрироваться");

                Log.e(TAG, "Registration failed", t);
                Toast.makeText(RegisterActivity.this,
                        "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}