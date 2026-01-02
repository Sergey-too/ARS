package com.example.ars.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.ars.models.User;
import com.google.gson.Gson;

public class SharedPreferencesHelper {
    private static final String PREF_NAME = "GardenAppPrefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER = "user_data";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private SharedPreferences preferences;
    private Gson gson;

    public SharedPreferencesHelper(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    // Токен
    public void saveToken(String token) {
        preferences.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return preferences.getString(KEY_TOKEN, null);
    }

    public void removeToken() {
        preferences.edit().remove(KEY_TOKEN).apply();
    }

    // Данные пользователя
    public void saveUser(User user) {
        String userJson = gson.toJson(user);
        preferences.edit().putString(KEY_USER, userJson).apply();
    }

    public User getUser() {
        String userJson = preferences.getString(KEY_USER, null);
        if (userJson != null) {
            return gson.fromJson(userJson, User.class);
        }
        return null;
    }

    public void removeUser() {
        preferences.edit().remove(KEY_USER).apply();
    }

    // Статус авторизации
    public void setLoggedIn(boolean isLoggedIn) {
        preferences.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply();
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // Очистить все данные
    public void clearAll() {
        preferences.edit().clear().apply();
    }
}