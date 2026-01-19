package com.example.ars;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ars.utils.SharedPreferencesHelper;

public abstract class BaseActivity extends AppCompatActivity {
    protected SharedPreferencesHelper prefsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsHelper = new SharedPreferencesHelper(this);

        // Проверяем авторизацию для защищенных экранов
        if (requiresAuth() && !isLoggedIn()) {
            redirectToLogin();
        }
    }

    protected boolean requiresAuth() {
        // Переопределить в дочерних классах если экран требует авторизации
        return false;
    }

    protected boolean isLoggedIn() {
        return prefsHelper.isLoggedIn() && prefsHelper.getToken() != null;
    }

    protected void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    protected String getToken() {
        return prefsHelper.getToken();
    }

    protected com.example.ars.models.User getCurrentUser() {
        return prefsHelper.getUser();
    }

    protected void logout() {
        prefsHelper.clearAll();
        redirectToLogin();
    }
}