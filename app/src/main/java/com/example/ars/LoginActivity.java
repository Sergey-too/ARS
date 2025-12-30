package com.example.ars;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ars.data_base.DatabaseHelper;

public class LoginActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация DatabaseHelper
        dbHelper = new DatabaseHelper();

        // Находим все элементы
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Обработчик кнопки ВОЙТИ
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        // Обработчик "Зарегистрироваться" (переход на RegisterActivity)
        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        // Обработчик "Забыли пароль?"
        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LoginActivity.this,
                        "Восстановление пароля будет позже", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performLogin() {
        String username = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Валидация
        if (username.isEmpty()) {
            etEmail.setError("Введите логин");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Введите пароль");
            etPassword.requestFocus();
            return;
        }

        // Показываем прогресс
        Toast.makeText(this, "Проверяем данные...", Toast.LENGTH_SHORT).show();

        // Запускаем в фоновом потоке
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean loginSuccess = dbHelper.loginUser(username, password);

                // Возвращаемся в UI поток для обновления интерфейса
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (loginSuccess) {
                            Toast.makeText(LoginActivity.this,
                                    "Вход выполнен успешно!", Toast.LENGTH_SHORT).show();

                            // Переходим на главный экран
                            Intent intent = new Intent(LoginActivity.this, PlantsActivity.class);
                            startActivity(intent);
                            finish(); // Закрываем экран входа
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "Неверный логин или пароль", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }).start();
    }
}