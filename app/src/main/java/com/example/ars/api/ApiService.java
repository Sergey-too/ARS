package com.example.ars.api;

import com.example.ars.models.Area;
import com.example.ars.models.AuthResponse;
import com.example.ars.models.Category;
import com.example.ars.models.CompatibilityDTO;
import com.example.ars.models.Crop;
import com.example.ars.models.DeleteResponse;
import com.example.ars.models.IndividualUserCrop;
import com.example.ars.models.Region;
import com.example.ars.models.SupportRequest;
import com.example.ars.models.User;
import com.example.ars.models.UserCrop;
import com.example.ars.models.WeatherComparisonDTO;
import com.example.ars.models.WeatherResponse;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

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

    // НОВЫЙ МЕТОД: получить ВСЕ данные по региону для админки
    @GET("/api/weather/admin/all/{regionName}")
    Call<WeatherResponse> getAllWeatherForRegion(@Path("regionName") String regionName);

    @GET("/api/regions/test")
    Call<String> createTestRegions();

    @GET("/api/weather/test-data")
    Call<String> addTestWeatherData();

    // 1. Получить все категории
    @GET("/api/categories")
    Call<List<Category>> getCategories();

    // 2. Получить растения по названию категории
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

    // 6. Получить растение по ID (для детальной страницы)
    @GET("/api/crops/{id}")
    Call<Crop> getCropById(@Path("id") Integer id);

    // Удаление данных о погоде
    @DELETE("/api/weather/delete/{id}")
    Call<DeleteResponse> deleteWeatherById(@Path("id") Integer id);

    @DELETE("/api/weather/delete-by-date-region")
    Call<DeleteResponse> deleteWeatherByDateRegion(
            @Query("date") String date,
            @Query("regionName") String regionName);

    // 7. Добавить новое растение
    @POST("/api/crops")
    Call<Crop> addCrop(@Body Crop crop);

    // 8. Обновить растение
    @PUT("/api/crops/{id}")
    Call<Crop> updateCrop(@Path("id") Integer id, @Body Crop crop);

    // 9. Удалить растение
    @DELETE("/api/crops/{id}")
    Call<Void> deleteCrop(@Path("id") Integer id);

    @DELETE("/api/crops/user/{userId}/{cropId}")
    Call<Map<String, Object>> deleteUserCrop(
            @Path("userId") Integer userId,
            @Path("cropId") Integer cropId);

    @DELETE("/api/crops/user/all/{userId}")
    Call<Map<String, Object>> deleteAllUserCrops(@Path("userId") Integer userId);

    @GET("/api/areas/user/{userId}")
    Call<List<Area>> getUserAreas(@Path("userId") Integer userId);

    @POST("/api/areas/add")
    Call<Map<String, Object>> addArea(@Body Map<String, Object> request);

    @PUT("/api/areas/update/{id}")
    Call<Map<String, Object>> updateArea(@Path("id") Integer areaId, @Body Map<String, Object> request);
    @DELETE("/api/areas/delete/{id}")
    Call<Map<String, Object>> deleteArea(@Path("id") Integer areaId);

    @GET("/api/weather/compare/{regionId}")
    Call<List<WeatherComparisonDTO>> getWeatherComparison(@Path("regionId") Long regionId);
    @GET("/api/weather/regions")
    Call<List<Region>> getAllRegions();

    @GET("api/crops/compatibility")
    Call<List<CompatibilityDTO>> getCompatibilityMatrix();

    @GET("api/support/user/{userId}")
    Call<List<SupportRequest>> getUserRequests(@Path("userId") Integer userId);

    @POST("api/support")
    Call<SupportRequest> createSupportRequest(@Body SupportRequest request);

    @PUT("api/support/{id}")
    Call<SupportRequest> updateSupportRequest(@Path("id") Integer id, @Body SupportRequest request);

    @DELETE("api/support/{id}")
    Call<Void> deleteSupportRequest(@Path("id") Integer id);

    @GET("api/crops")
    Call<List<Crop>> getAllCrops();

    @GET("api/users")
    Call<List<User>> getAllUsers();

    @PUT("api/users/{id}/toggle-admin")
    Call<Void> toggleAdmin(@Path("id") int userId);

    @PUT("api/users/{id}/toggle-ban")
    Call<Void> toggleBan(@Path("id") int userId);

    @PUT("api/users/{id}")
    Call<Void> updateUser(@Path("id") int userId, @Body User user);

    @POST("api/regions")
    Call<Region> createRegion(@Body Region region);

    @PUT("api/regions/{id}")
    Call<Region> updateRegion(@Path("id") Long id, @Body Region region);

    @GET("api/my-crops/user/{userId}")
    Call<List<IndividualUserCrop>> getIndividualUserCrops(@Path("userId") Integer userId);

    @GET("api/my-crops/{id}")
    Call<IndividualUserCrop> getUserCropById(@Path("id") Integer id);

    @POST("api/my-crops")
    Call<IndividualUserCrop> createUserCrop(@Body IndividualUserCrop crop);

    @PUT("api/my-crops/{id}")
    Call<IndividualUserCrop> updateUserCrop(@Path("id") Integer id, @Body IndividualUserCrop crop);

    @DELETE("api/my-crops/{id}")
    Call<Void> deleteUserCrop(@Path("id") Integer id);

    @GET("api/my-crops/user/{userId}")
    Call<List<UserCrop>> getIndividualCrops(@Path("userId") int userId);
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