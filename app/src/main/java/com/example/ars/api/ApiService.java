package com.example.ars.api;

import com.example.ars.models.ActionType;
import com.example.ars.models.Area;
import com.example.ars.models.AuthResponse;
import com.example.ars.models.Category;
import com.example.ars.models.CompatibilityDTO;
import com.example.ars.models.Crop;
import com.example.ars.models.DeleteResponse;
import com.example.ars.models.GardenHistory;
import com.example.ars.models.History;
import com.example.ars.models.IndividualUserCrop;
import com.example.ars.models.Region;
import com.example.ars.models.SupportMessage;
import com.example.ars.models.SupportRequest;
import com.example.ars.models.TaskItem;
import com.example.ars.models.User;
import com.example.ars.models.UserCrop;
import com.example.ars.models.WeatherAlert;
import com.example.ars.models.WeatherComparisonDTO;
import com.example.ars.models.WeatherData;
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

    // Auth
    @POST("/api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest loginRequest);

    @POST("/api/auth/register")
    Call<AuthResponse> register(@Body User user);

    @POST("/api/auth/logout")
    Call<AuthResponse> logout(@Header("Authorization") String token);

    @GET("/api/auth/profile")
    Call<User> getProfile(@Header("Authorization") String token);

    // Weather
    @GET("/api/weather/regions")
    Call<List<Region>> getRegions();

    @GET("/api/weather/android/{regionName}")
    Call<WeatherResponse> getWeatherForRegion(@Path("regionName") String regionName);

    @GET("/api/weather/admin/all/{regionName}")
    Call<WeatherResponse> getAllWeatherForRegion(@Path("regionName") String regionName);

    @GET("/api/regions/test")
    Call<String> createTestRegions();

    @GET("/api/weather/test-data")
    Call<String> addTestWeatherData();

    @GET("/api/weather/compare/{regionId}")
    Call<List<WeatherComparisonDTO>> getWeatherComparison(@Path("regionId") Long regionId);

    @GET("/api/weather/all")
    Call<List<WeatherData>> getAllWeather();

    @DELETE("/api/weather/{id}")
    Call<Void> deleteWeather(@Path("id") Integer id);

    @GET("/api/weather/region/{regionId}")
    Call<WeatherResponse> getWeatherByRegionId(@Path("regionId") int regionId);

    @DELETE("/api/weather/delete/{id}")
    Call<DeleteResponse> deleteWeatherById(@Path("id") Integer id);

    @DELETE("/api/weather/delete-by-date-region")
    Call<DeleteResponse> deleteWeatherByDateRegion(
            @Query("date") String date,
            @Query("regionName") String regionName);

    // Categories
    @GET("/api/categories")
    Call<List<Category>> getCategories();

    @POST("/api/categories")
    Call<Category> createCategory(@Body Category category);

    @PUT("/api/categories/{id}")
    Call<Category> updateCategory(@Path("id") Integer id, @Body Category category);

    @DELETE("/api/categories/{id}")
    Call<Void> deleteCategory(@Path("id") Integer id);

    // Crops (system)
    @GET("/api/crops")
    Call<List<Crop>> getAllCrops();

    @GET("/api/crops/{id}")
    Call<Crop> getCropById(@Path("id") Integer id);

    @GET("/api/crops/by-category/{categoryName}")
    Call<List<Crop>> getCropsByCategory(@Path("categoryName") String categoryName);

    @POST("/api/crops")
    Call<Crop> addCrop(@Body Crop crop);

    @PUT("/api/crops/{id}")
    Call<Crop> updateCrop(@Path("id") Integer id, @Body Crop crop);

    @DELETE("/api/crops/{id}")
    Call<Void> deleteCrop(@Path("id") Integer id);

    @GET("/api/crops/compatibility")
    Call<List<CompatibilityDTO>> getCompatibilityMatrix();

    @POST("api/compatibility/update")
    Call<Void> updateCompatibility(@Body CompatibilityDTO dto);

    // User Crops
    @GET("/api/crops/user/{userId}")
    Call<List<UserCrop>> getUserCrops(@Path("userId") Integer userId);

    @POST("/api/crops/user/add")
    Call<Map<String, Object>> addUserCrop(@Body Map<String, Object> request);

    @DELETE("/api/crops/user/{userId}/{cropId}")
    Call<Map<String, Object>> deleteUserCrop(
            @Path("userId") Integer userId,
            @Path("cropId") Integer cropId);

    @DELETE("/api/crops/user/all/{userId}")
    Call<Map<String, Object>> deleteAllUserCrops(@Path("userId") Integer userId);

    // Individual User Crops
    @GET("/api/my-crops/user/{userId}")
    Call<List<IndividualUserCrop>> getIndividualUserCrops(@Path("userId") Integer userId);

    @GET("/api/my-crops/{id}")
    Call<IndividualUserCrop> getUserCropById(@Path("id") int id);

    @POST("/api/my-crops")
    Call<IndividualUserCrop> createUserCrop(@Body IndividualUserCrop crop);

    @PUT("/api/my-crops/{id}")
    Call<IndividualUserCrop> updateUserCrop(@Path("id") int id, @Body IndividualUserCrop crop);

    @DELETE("/api/my-crops/{id}")
    Call<Void> deleteUserCrop(@Path("id") int id);

    // Areas
    @GET("/api/areas/user/{userId}")
    Call<List<Area>> getUserAreas(@Path("userId") Integer userId);

    @POST("/api/areas/add")
    Call<Map<String, Object>> addArea(@Body Map<String, Object> request);

    @PUT("/api/areas/update/{id}")
    Call<Map<String, Object>> updateArea(@Path("id") Integer areaId, @Body Map<String, Object> request);

    @DELETE("/api/areas/delete/{id}")
    Call<Map<String, Object>> deleteArea(@Path("id") Integer areaId);

    // Regions
    @GET("/api/regions")
    Call<List<Region>> getAllRegions();

    @POST("/api/regions")
    Call<Region> createRegion(@Body Region region);

    @PUT("/api/regions/{id}")
    Call<Region> updateRegion(@Path("id") Integer id, @Body Region region);

    @DELETE("/api/regions/{id}")
    Call<Void> deleteRegion(@Path("id") Integer id);

    // Support
    @GET("/api/support/user/{userId}")
    Call<List<SupportRequest>> getUserRequests(@Path("userId") Integer userId);

    @POST("/api/support")
    Call<SupportRequest> createSupportRequest(@Body SupportRequest request);

    @PUT("/api/support/{id}")
    Call<SupportRequest> updateSupportRequest(@Path("id") Integer id, @Body SupportRequest request);

    @DELETE("/api/support/{id}")
    Call<Void> deleteSupportRequest(@Path("id") Integer id);

    // Users (admin)
    @GET("/api/users")
    Call<List<User>> getAllUsers();

    @PUT("/api/users/{id}/toggle-admin")
    Call<Void> toggleAdmin(@Path("id") int userId);

    @PUT("/api/users/{id}/toggle-ban")
    Call<Void> toggleBan(@Path("id") int userId);

    @PUT("/api/users/{id}")
    Call<Void> updateUser(@Path("id") int userId, @Body User user);

    @Multipart
    @POST("/api/files/upload/crop")
    Call<String> uploadCropImage(
            @Part MultipartBody.Part file,
            @Part("category") RequestBody category
    );

    //Tasks
    @POST("/api/history/add")
    Call<Map<String, Object>> addGardenHistory(@Body Map<String, Object> request);

    @GET("/api/tasks/user/{userId}/weekly")
    Call<List<TaskItem>> getWeeklyTasks(@Path("userId") Integer userId);

    @GET("/api/history/user/{userId}/planting")
    Call<List<GardenHistory>> getPlantingHistory(@Path("userId") Integer userId);

    @POST("/api/history/plant")
    Call<Map<String, Object>> plantCrop(@Body Map<String, Object> request);

    @POST("/api/tasks/complete")
    Call<Map<String, Object>> completeTask(@Body Map<String, Object> request);

    //History
    @GET("api/history/user/{userId}")
    Call<List<History>> getHistory(@Path("userId") int userId);

    @GET("/api/weather/by-date/{regionId}/{date}")
    Call<WeatherData> getWeatherByDate(@Path("regionId") int regionId, @Path("date") String date);

    // Action Types
    @GET("/api/action-types")
    Call<List<ActionType>> getActionTypes();

    // SupportMessage
    @GET("api/support/user/{userId}")
    Call<List<SupportRequest>> getUserRequests(@Path("userId") int userId);

    @GET("api/support/admin/all")
    Call<List<SupportRequest>> getAllRequests();

    @PUT("api/support/{id}/status")
    Call<SupportRequest> updateRequestStatus(@Path("id") int id, @Query("statusId") int statusId);

    @DELETE("api/support/{id}")
    Call<Void> deleteSupportRequest(@Path("id") int id);

    @GET("api/support/{requestId}/messages")
    Call<List<SupportMessage>> getChatMessages(@Path("requestId") int requestId);

    @POST("api/support/messages")
    Call<SupportMessage> sendChatMessage(@Body SupportMessage message);

    //WeatherAlert
    @GET("api/alerts/check/{userId}")
    Call<List<WeatherAlert>> checkAlerts(@Path("userId") int userId);

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