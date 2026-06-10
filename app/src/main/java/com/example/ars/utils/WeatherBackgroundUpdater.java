package com.example.ars.utils;

import android.content.Context;
import android.util.Log;

import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.Region;
import com.example.ars.models.WeatherResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherBackgroundUpdater {
    private static final String TAG = "WeatherBackgroundUpdater";
    private final ApiService apiService;
    private final WeatherCacheManager cacheManager;

    public WeatherBackgroundUpdater(Context context) {
        this.apiService = RetrofitClient.getApiService();
        this.cacheManager = new WeatherCacheManager(context);
    }

    public void updateAllData() {
        updateRegionsAndWeather();
    }

    private void updateRegionsAndWeather() {
        apiService.getRegions().enqueue(new Callback<List<Region>>() {
            @Override
            public void onResponse(Call<List<Region>> call, Response<List<Region>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<Region> regions = response.body();

                    cacheManager.saveRegions(regions, new WeatherCacheManager.VoidCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Regions saved to cache");
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Save regions error: " + error);
                        }
                    });

                    for (Region region : regions) {
                        updateWeatherForRegion(region);
                    }
                } else {
                    Log.e(TAG, "Failed to load regions");
                }
            }

            @Override
            public void onFailure(Call<List<Region>> call, Throwable t) {
                Log.e(TAG, "API error: " + t.getMessage());
            }
        });
    }

    private void updateWeatherForRegion(Region region) {
        apiService.getWeatherForRegion(region.getName()).enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getWeather() != null) {
                    cacheManager.saveForecast(region.getId(), response.body().getWeather(),
                            new WeatherCacheManager.VoidCallback() {
                                @Override
                                public void onSuccess() {
                                    Log.d(TAG, "Weather saved for region: " + region.getName());
                                }

                                @Override
                                public void onError(String error) {
                                    Log.e(TAG, "Save weather error for " + region.getName() + ": " + error);
                                }
                            });
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Log.e(TAG, "Weather API error for " + region.getName() + ": " + t.getMessage());
            }
        });
    }
}