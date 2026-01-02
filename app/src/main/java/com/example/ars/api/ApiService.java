// ApiService.java
package com.example.ars.api;

import com.example.ars.models.AuthResponse;
import com.example.ars.models.Region;
import com.example.ars.models.User;
import com.example.ars.models.WeatherResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    // Аутентификация
    @POST("/api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest loginRequest);

    @POST("/api/auth/register")
    Call<AuthResponse> register(@Body User user);

    @POST("/api/auth/logout")
    Call<AuthResponse> logout(@Header("Authorization") String token);

    @GET("/api/auth/profile")
    Call<User> getProfile(@Header("Authorization") String token);

    // Существующие эндпоинты погоды
    @GET("/api/weather/regions")
    Call<List<Region>> getRegions();

    @GET("/api/weather/android/{regionName}")
    Call<WeatherResponse> getWeatherForRegion(@Path("regionName") String regionName);

    @GET("/api/regions/test")
    Call<String> createTestRegions();

    @GET("/api/weather/test-data")
    Call<String> addTestWeatherData();

    // Вспомогательный класс для запроса входа
    class LoginRequest {
        private String identifier;
        private String password;

        public LoginRequest(String identifier, String password) {
            this.identifier = identifier;
            this.password = password;
        }

        public String getIdentifier() {
            return identifier;
        }

        public String getPassword() {
            return password;
        }
    }
}