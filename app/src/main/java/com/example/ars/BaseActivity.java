package com.example.ars;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ars.models.User;
import com.example.ars.utils.SharedPreferencesHelper;

public abstract class BaseActivity extends AppCompatActivity {
    protected SharedPreferencesHelper prefsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsHelper = new SharedPreferencesHelper(this);

        if (requiresAuth() && !isLoggedIn()) {
            redirectToLogin();
            return;
        }

        if (requiresAdmin() && !isAdmin()) {
            Toast.makeText(this, "Доступ запрещён. Требуются права администратора.", Toast.LENGTH_LONG).show();
            redirectToLogin();
            return;
        }

        if (isBanned()) {
            Toast.makeText(this, "Ваш аккаунт заблокирован.", Toast.LENGTH_LONG).show();
            redirectToLogin();
        }
    }

    protected boolean requiresAuth() {
        return true;
    }

    protected boolean requiresAdmin() {
        return false;
    }

    protected boolean isLoggedIn() {
        return prefsHelper.isLoggedIn() && prefsHelper.getToken() != null;
    }

    protected boolean isAdmin() {
        User user = prefsHelper.getUser();
        return user != null && user.getIsAdmin();
    }

    protected boolean isBanned() {
        User user = prefsHelper.getUser();
        return user != null && user.getInBan();
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

    protected User getCurrentUser() {
        return prefsHelper.getUser();
    }

    protected void logout() {
        prefsHelper.clearAll();
        redirectToLogin();
    }
}