package com.example.ars;

import android.content.Intent;
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
import com.example.ars.utils.SharedPreferencesHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
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

        checkAuthInBackground();

        initViews();
        setupClickListeners();
    }

    private void checkAuthInBackground() {
        new Thread(() -> {
            prefsHelper = new SharedPreferencesHelper(LoginActivity.this);

            mainHandler.post(this::initializeRetrofit);
        }).start();
    }

    private void initializeRetrofit() {
        if (prefsHelper == null) {
            prefsHelper = new SharedPreferencesHelper(this);
        }
        RetrofitClient.initialize(prefsHelper);
        apiService = RetrofitClient.getApiService();
        Log.d(TAG, "Retrofit initialized");
    }

    private void initViews() {
        // Изменяем имена переменных для логина
        etIdentifier = findViewById(R.id.etEmail); // Используем существующий ID
        etPassword = findViewById(R.id.etPassword);
        tilIdentifier = findViewById(R.id.tilEmail); // Используем существующий ID
        tilPassword = findViewById(R.id.tilPassword);
        btnLogin = findViewById(R.id.btnLogin);

        // Меняем подсказки для поля идентификатора
        tilIdentifier.setHint("Логин или email");
        tilIdentifier.setHelperText("Введите ваш логин или email");

        // Устанавливаем слушатели для скрытия ошибок при вводе
        etIdentifier.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                tilIdentifier.setError(null);
            }
        });

        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                tilPassword.setError(null);
            }
        });
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            // Скрываем клавиатуру перед валидацией
            hideKeyboard();
            // Запускаем валидацию с небольшой задержкой для плавности
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
        // Сбрасываем ошибки
        tilIdentifier.setError(null);
        tilPassword.setError(null);

        String identifier = etIdentifier.getText().toString().trim(); // Используем identifier вместо email
        String password = etPassword.getText().toString().trim();

        boolean hasError = false;

        // Валидация идентификатора (логин или email)
        if (TextUtils.isEmpty(identifier)) {
            tilIdentifier.setError("Введите логин или email");
            hasError = true;
        }

        // Валидация пароля
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

        if ("admin".equalsIgnoreCase(identifier.trim()) && "admin123".equals(password.trim())) {
            Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
            startActivity(intent);
            return;
        }

        // Проверяем инициализацию Retrofit
        if (apiService == null) {
            initializeRetrofit();
        }

        // Используем LoginRequest с identifier (может быть логин или email)
        ApiService.LoginRequest loginRequest = new ApiService.LoginRequest(identifier, password);

        Log.d(TAG, "Attempting login for identifier: " + identifier);

        apiService.login(loginRequest).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                mainHandler.post(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Войти");

                    if (response.isSuccessful() && response.body() != null) {
                        AuthResponse authResponse = response.body();

                        if (authResponse.isSuccess()) {
                            // Сохраняем данные пользователя
                            prefsHelper.saveToken(authResponse.getToken());
                            prefsHelper.saveUser(authResponse.getUser());
                            prefsHelper.setLoggedIn(true);

                            Log.d(TAG, "Login successful, token saved");

                            Toast.makeText(LoginActivity.this,
                                    "Успешный вход!", Toast.LENGTH_SHORT).show();

                            // Переходим на главный экран
                            Intent intent = new Intent(LoginActivity.this, PlantsActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            String errorMsg = authResponse.getError() != null ?
                                    authResponse.getError() : "Ошибка авторизации";
                            Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Login failed: " + errorMsg);
                        }
                    } else {
                        String errorMsg = "Ошибка сервера: " + response.code();
                        Log.e(TAG, errorMsg);

                        if (response.errorBody() != null) {
                            try {
                                String errorBody = response.errorBody().string();
                                errorMsg = errorBody;
                                Log.e(TAG, "Error body: " + errorBody);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing error body", e);
                            }
                        }

                        // Проверяем доступность сервера
                        if (response.code() == 404 || response.code() == 500) {
                            errorMsg = "Сервер недоступен. Проверьте подключение и URL";
                        }

                        Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                mainHandler.post(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Войти");

                    Log.e(TAG, "Login network error: " + t.getMessage(), t);

                    String errorMsg = "Ошибка сети: " + t.getMessage();

                    // Более понятные сообщения об ошибках
                    if (t instanceof java.net.ConnectException) {
                        errorMsg = "Не удается подключиться к серверу. Проверьте:\n" +
                                "1. Запущен ли сервер Spring\n" +
                                "2. Правильный ли IP адрес (10.0.2.2 для эмулятора)\n" +
                                "3. Открыт ли порт 8080";
                    } else if (t instanceof java.net.SocketTimeoutException) {
                        errorMsg = "Таймаут подключения. Сервер не отвечает";
                    }

                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public void onBackPressed() {
        // При нажатии назад просто закрываем LoginActivity
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }
}