package com.example.ars;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ars.data_base.DatabaseHelper;

public class RegisterActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private CheckBox cbTerms;
    private Button btnRegister;
    private TextView tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Инициализация DatabaseHelper
        dbHelper = new DatabaseHelper();

        // Находим все элементы
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        cbTerms = findViewById(R.id.cbTerms);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        // Обработчик кнопки ЗАРЕГИСТРИРОВАТЬСЯ
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegistration();
            }
        });

        // Обработчик "Войти" (возврат на LoginActivity)
        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Закрываем этот экран и возвращаемся к вводу
            }
        });
    }

    private void performRegistration() {
        String username = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Валидация
        if (username.isEmpty()) {
            etName.setError("Введите логин");
            etName.requestFocus();
            return;
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Введите корректный email");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty() || password.length() < 6) {
            etPassword.setError("Пароль должен быть не менее 6 символов");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Пароли не совпадают");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Примите условия использования", Toast.LENGTH_SHORT).show();
            cbTerms.requestFocus();
            return;
        }

        // Показываем прогресс
        Toast.makeText(this, "Регистрируем пользователя...", Toast.LENGTH_SHORT).show();

        // Запускаем в фоновом потоке
        new Thread(new Runnable() {
            @Override
            public void run() {
                String errorMessage = dbHelper.registerUser(username, email, password, confirmPassword);

                // Возвращаемся в UI поток
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (errorMessage == null) {
                            // Регистрация успешна
                            Toast.makeText(RegisterActivity.this,
                                    "Регистрация успешна! Теперь вы можете войти.",
                                    Toast.LENGTH_LONG).show();

                            // Очищаем поля
                            etName.setText("");
                            etEmail.setText("");
                            etPassword.setText("");
                            etConfirmPassword.setText("");
                            cbTerms.setChecked(false);

                            // Возвращаемся на экран входа через 2 секунды
                            new android.os.Handler().postDelayed(
                                    new Runnable() {
                                        public void run() {
                                            finish();
                                        }
                                    },
                                    2000);

                        } else {
                            // Показываем ошибку
                            Toast.makeText(RegisterActivity.this,
                                    "Ошибка: " + errorMessage,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }).start();
    }
}