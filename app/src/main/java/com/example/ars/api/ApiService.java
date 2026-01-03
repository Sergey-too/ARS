package com.example.ars.api;

import com.example.ars.models.AuthResponse;
import com.example.ars.models.Category;
import com.example.ars.models.Crop;
import com.example.ars.models.Region;
import com.example.ars.models.User;
import com.example.ars.models.UserCrop;
import com.example.ars.models.WeatherResponse;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
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

    // Регионы и погода
    @GET("/api/weather/regions")
    Call<List<Region>> getRegions();

    @GET("/api/weather/android/{regionName}")
    Call<WeatherResponse> getWeatherForRegion(@Path("regionName") String regionName);

    @GET("/api/regions/test")
    Call<String> createTestRegions();

    @GET("/api/weather/test-data")
    Call<String> addTestWeatherData();

    // 1. Получить все категории
    @GET("/api/categories")
    Call<List<Category>> getCategories();

    // 2. Получить растения по названию категории (ОСНОВНОЙ метод)
    @GET("/api/crops/by-category/{categoryName}")
    Call<List<Crop>> getCropsByCategory(@Path("categoryName") String categoryName);

    // 3. Получить растения пользователя
    @GET("/api/crops/user/{userId}")
    Call<List<UserCrop>> getUserCrops(@Path("userId") Integer userId);

    // 4. Добавить растение пользователю
    @POST("/api/crops/user/add")
    Call<Map<String, Object>> addUserCrop(@Body Map<String, Object> request);

    // 5. Загрузка фото растения
    @Multipart
    @POST("/api/files/upload/crop")
    Call<String> uploadCropImage(
            @Part MultipartBody.Part file,
            @Part("category") RequestBody category
    );

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