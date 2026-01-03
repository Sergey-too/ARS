// RetrofitClient.java - исправленная версия
package com.example.ars.api;

import android.util.Log;
import com.example.ars.utils.SharedPreferencesHelper;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class RetrofitClient {
    private static final String TAG = "RetrofitClient";
    public static final String BASE_URL = "http://10.0.2.2:8080"; // Для эмулятора
    //private static final String BASE_URL = "http://192.168.100.13:8080"; // Для телефона


    private static Retrofit retrofit = null;
    private static SharedPreferencesHelper prefsHelper;

    public static void initialize(SharedPreferencesHelper helper) {
        prefsHelper = helper;
        Log.d(TAG, "RetrofitClient initialized");
    }

    public static ApiService getApiService() {
        if (retrofit == null) {
            Log.d(TAG, "Creating new Retrofit instance");

            // Логирование HTTP запросов
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

            // Добавляем логирование
            httpClient.addInterceptor(loggingInterceptor);

            // Добавляем интерцептор для авторизации
            httpClient.addInterceptor(chain -> {
                Request original = chain.request();

                // Добавляем токен если есть
                String token = null;
                if (prefsHelper != null) {
                    token = prefsHelper.getToken();
                    Log.d(TAG, "Current token: " + (token != null ? "present" : "null"));
                }

                Request.Builder requestBuilder = original.newBuilder();

                if (token != null && !token.isEmpty()) {
                    requestBuilder.header("Authorization", "Bearer " + token);
                    Log.d(TAG, "Added Authorization header");
                }

                requestBuilder.header("Content-Type", "application/json");
                requestBuilder.header("Accept", "application/json");

                return chain.proceed(requestBuilder.build());
            });

            // Таймауты
            httpClient.connectTimeout(30, TimeUnit.SECONDS);
            httpClient.readTimeout(30, TimeUnit.SECONDS);
            httpClient.writeTimeout(30, TimeUnit.SECONDS);

            // Создаем Retrofit
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(httpClient.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            Log.d(TAG, "Retrofit instance created with base URL: " + BASE_URL);
        }

        return retrofit.create(ApiService.class);
    }
}