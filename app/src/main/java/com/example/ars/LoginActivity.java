package com.example.ars;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
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
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private TextInputEditText etIdentifier, etPassword;
    private TextInputLayout tilIdentifier, tilPassword;
    private Button btnLogin;
    private SharedPreferencesHelper prefsHelper;
    private ApiService apiService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainHandler = new Handler(Looper.getMainLooper());

        prefsHelper = new SharedPreferencesHelper(this);

        // Проверяем, был ли уже вход
        if (prefsHelper.isLoggedIn() && prefsHelper.getToken() != null) {
            User user = prefsHelper.getUser();
            if (user != null && user.getIsAdmin()) {
                startActivity(new Intent(this, AdminDashboardActivity.class));
            } else if (user != null) {
                startActivity(new Intent(this, PlantsActivity.class));
            }
            finish();
            return;
        }

        initializeRetrofit();
        initViews();
        setupClickListeners();
    }

    private void initializeRetrofit() {
        RetrofitClient.initialize(prefsHelper);
        apiService = RetrofitClient.getApiService();
        Log.d(TAG, "Retrofit initialized");
    }

    private void initViews() {
        etIdentifier = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tilIdentifier = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        btnLogin = findViewById(R.id.btnLogin);

        tilIdentifier.setHint("Логин или email");
        tilIdentifier.setHelperText("Введите ваш логин или email");

        etIdentifier.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tilIdentifier.setError(null);
        });

        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tilPassword.setError(null);
        });
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            hideKeyboard();
            mainHandler.postDelayed(this::attemptLogin, 100);
        });

        TextView tvRegister = findViewById(R.id.tvRegister);
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Функция восстановления пароля в разработке", Toast.LENGTH_SHORT).show();
        });
    }

    private void hideKeyboard() {
        if (getCurrentFocus() != null) {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void attemptLogin() {
        tilIdentifier.setError(null);
        tilPassword.setError(null);

        String identifier = etIdentifier.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        boolean hasError = false;

        if (TextUtils.isEmpty(identifier)) {
            tilIdentifier.setError("Введите логин или email");
            hasError = true;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Введите пароль");
            hasError = true;
        } else if (password.length() < 6) {
            tilPassword.setError("Пароль должен содержать минимум 6 символов");
            hasError = true;
        }

        if (!hasError) {
            performLogin(identifier, password);
        }
    }

    private void performLogin(String identifier, String password) {
        btnLogin.setEnabled(false);
        btnLogin.setText("Вход...");

        if (apiService == null) {
            initializeRetrofit();
        }

        ApiService.LoginRequest loginRequest = new ApiService.LoginRequest(identifier, password);

        apiService.login(loginRequest).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                mainHandler.post(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Войти");

                    if (response.isSuccessful() && response.body() != null) {
                        AuthResponse authResponse = response.body();

                        if (authResponse.isSuccess() && authResponse.getUser() != null) {
                            User user = authResponse.getUser();

                            if (user.getInBan()) {
                                Toast.makeText(LoginActivity.this,
                                        "Ваш аккаунт заблокирован. Обратитесь к администратору.",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }

                            prefsHelper.saveToken(authResponse.getToken());
                            prefsHelper.saveUser(user);
                            prefsHelper.setLoggedIn(true);

                            if (user.getIsAdmin()) {
                                Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                                startActivity(intent);
                            } else {
                                Intent intent = new Intent(LoginActivity.this, PlantsActivity.class);
                                startActivity(intent);
                            }
                            finish();
                        } else {
                            String errorMsg = authResponse.getError() != null ?
                                    authResponse.getError() : "Неверный логин или пароль";
                            Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        handleErrorResponse(response);
                    }
                });
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                mainHandler.post(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Войти");
                    Toast.makeText(LoginActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void handleErrorResponse(Response<AuthResponse> response) {
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                if (errorBody.contains("banned") || errorBody.contains("заблокирован")) {
                    Toast.makeText(LoginActivity.this, "Аккаунт заблокирован", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(LoginActivity.this, "Ошибка: " + errorBody, Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(LoginActivity.this, "Ошибка сервера: " + response.code(), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(LoginActivity.this, "Ошибка сервера", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }
}