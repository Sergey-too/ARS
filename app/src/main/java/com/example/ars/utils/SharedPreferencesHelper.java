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
    private static final String KEY_LAST_USER_ID = "last_user_id";
    private static final String KEY_LAST_LOGIN = "last_login";

    private SharedPreferences preferences;
    private Gson gson;

    public SharedPreferencesHelper(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveToken(String token) {
        preferences.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return preferences.getString(KEY_TOKEN, null);
    }

    public void removeToken() {
        preferences.edit().remove(KEY_TOKEN).apply();
    }

    public void saveUser(User user) {
        String userJson = gson.toJson(user);
        preferences.edit().putString(KEY_USER, userJson).apply();
        if (user != null && user.getId() != null) {
            saveLastUserId(user.getId());
            saveLastLogin(String.valueOf(System.currentTimeMillis()));
        }
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

    public void setLoggedIn(boolean isLoggedIn) {
        preferences.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply();
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void saveLastUserId(int userId) {
        preferences.edit().putInt(KEY_LAST_USER_ID, userId).apply();
    }

    public int getLastUserId() {
        return preferences.getInt(KEY_LAST_USER_ID, -1);
    }

    public void saveLastLogin(String timestamp) {
        preferences.edit().putString(KEY_LAST_LOGIN, timestamp).apply();
    }

    public String getLastLogin() {
        return preferences.getString(KEY_LAST_LOGIN, null);
    }

    public void saveIsAdmin(boolean isAdmin) {
        preferences.edit().putBoolean("is_admin", isAdmin).apply();
    }

    public boolean isAdmin() {
        return preferences.getBoolean("is_admin", false);
    }

    public void clearAll() {
        preferences.edit().clear().apply();
    }
}