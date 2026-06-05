package com.example.ars.api;

import android.util.Log;
import com.example.ars.utils.SharedPreferencesHelper;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory; // НАДО ДОБАВИТЬ В ДЕПЕНДЕНСИ
import java.util.concurrent.TimeUnit;

public class RetrofitClient {
    private static final String TAG = "RetrofitClient";
    public static final String BASE_URL = "http://192.168.100.16:8080";

    private static Retrofit retrofit = null;
    private static Retrofit fileRetrofit = null;
    public static SharedPreferencesHelper prefsHelper;

    public static void initialize(SharedPreferencesHelper helper) {
        prefsHelper = helper;
    }

    public static ApiService getApiService() {
        if (retrofit == null) {
            OkHttpClient.Builder httpClient = getBaseHttpClientBuilder();

            httpClient.addInterceptor(chain -> {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder();

                addAuthHeader(requestBuilder);

                requestBuilder.header("Content-Type", "application/json");
                requestBuilder.header("Accept", "application/json");

                return chain.proceed(requestBuilder.build());
            });

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(httpClient.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

    public static ApiService getFileApiService() {
        if (fileRetrofit == null) {
            OkHttpClient.Builder httpClient = getBaseHttpClientBuilder();

            httpClient.addInterceptor(chain -> {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder();

                addAuthHeader(requestBuilder);

                return chain.proceed(requestBuilder.build());
            });

            fileRetrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(httpClient.build())
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return fileRetrofit.create(ApiService.class);
    }

    private static OkHttpClient.Builder getBaseHttpClientBuilder() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.addInterceptor(loggingInterceptor);
        builder.connectTimeout(30, TimeUnit.SECONDS);
        builder.readTimeout(30, TimeUnit.SECONDS);
        builder.writeTimeout(30, TimeUnit.SECONDS);
        return builder;
    }

    private static void addAuthHeader(Request.Builder builder) {
        String token = null;
        if (prefsHelper != null) {
            token = prefsHelper.getToken();
        }
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }
    }
}