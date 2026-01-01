package com.example.ars.api;


import com.example.ars.models.Region;
import com.example.ars.models.WeatherResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {

    // Получить все регионы
    @GET("/api/weather/regions")
    Call<List<Region>> getRegions();

    // Получить погоду для региона (для Android UI)
    @GET("/api/weather/android/{regionName}")
    Call<WeatherResponse> getWeatherForRegion(@Path("regionName") String regionName);

    // Тестовые endpoints
    @GET("/api/regions/test")
    Call<String> createTestRegions();

    @GET("/api/weather/test-data")
    Call<String> addTestWeatherData();
}