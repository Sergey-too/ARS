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

        dbHelper = new DatabaseHelper();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        cbTerms = findViewById(R.id.cbTerms);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        // Кнопка регистрации
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etName.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();

                // Базовая валидация
                if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(RegisterActivity.this,
                            "Заполните все поля", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!cbTerms.isChecked()) {
                    Toast.makeText(RegisterActivity.this,
                            "Примите условия использования", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Запускаем в фоновом потоке
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String errorMessage = dbHelper.registerUser(
                                username, email, password, confirmPassword
                        );

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (errorMessage == null) {
                                    Toast.makeText(RegisterActivity.this,
                                            "Регистрация успешна!", Toast.LENGTH_SHORT).show();
                                    finish(); // Возвращаемся на экран входа
                                } else {
                                    Toast.makeText(RegisterActivity.this,
                                            errorMessage, Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    }
                }).start();
            }
        });

        // Переход к входу
        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Просто закрываем этот экран, возвращаемся к LoginActivity
            }
        });
    }
}