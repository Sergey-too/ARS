package com.example.ars.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.ars.localdb.AppDatabase;
import com.example.ars.localdb.RegionCacheEntity;
import com.example.ars.localdb.WeatherCacheEntity;
import com.example.ars.models.Region;
import com.example.ars.models.WeatherData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherCacheManager {
    private static final String TAG = "WeatherCacheManager";
    private static final long CACHE_EXPIRY_MS = 7 * 24 * 60 * 60 * 1000L;

    private final AppDatabase database;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public WeatherCacheManager(Context context) {
        this.database = AppDatabase.getInstance(context);
    }

    public interface VoidCallback {
        void onSuccess();
        default void onError(String error) {}
    }

    public interface CacheCallback<T> {
        void onSuccess(T result);
        default void onError(String error) {}
    }

    public void saveRegions(List<Region> regions, VoidCallback callback) {
        executor.execute(() -> {
            try {
                database.regionCacheDao().deleteAll();
                long now = System.currentTimeMillis();
                List<RegionCacheEntity> entities = new ArrayList<>();
                for (Region r : regions) {
                    entities.add(new RegionCacheEntity(r.getId(), r.getName(), now));
                }
                database.regionCacheDao().insertAll(entities);
                Log.d(TAG, "Saved " + entities.size() + " regions to cache");
                if (callback != null) {
                    mainHandler.post(callback::onSuccess);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving regions: " + e.getMessage());
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    public void getCachedRegions(CacheCallback<List<Region>> callback) {
        executor.execute(() -> {
            try {
                List<RegionCacheEntity> entities = database.regionCacheDao().getAllRegions();
                if (entities == null || entities.isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(null));
                    return;
                }
                List<Region> regions = new ArrayList<>();
                for (RegionCacheEntity entity : entities) {
                    Region r = new Region();
                    r.setId(entity.id);
                    r.setName(entity.name);
                    regions.add(r);
                }
                mainHandler.post(() -> callback.onSuccess(regions));
            } catch (Exception e) {
                Log.e(TAG, "Error getting cached regions: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void hasCachedRegions(CacheCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                int count = database.regionCacheDao().getCount();
                mainHandler.post(() -> callback.onSuccess(count > 0));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void saveForecast(int regionId, List<WeatherData> forecast, VoidCallback callback) {
        executor.execute(() -> {
            try {
                database.weatherCacheDao().deleteByRegion(regionId);

                long now = System.currentTimeMillis();
                List<WeatherCacheEntity> entities = new ArrayList<>();

                for (WeatherData wd : forecast) {
                    entities.add(new WeatherCacheEntity(regionId, wd, now));
                }

                database.weatherCacheDao().insertAll(entities);

                long expiryTime = System.currentTimeMillis() - CACHE_EXPIRY_MS;
                database.weatherCacheDao().deleteOld(expiryTime);

                Log.d(TAG, "Saved " + entities.size() + " weather records for region " + regionId);

                if (callback != null) {
                    mainHandler.post(callback::onSuccess);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving weather cache: " + e.getMessage());
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    public void getForecast(int regionId, CacheCallback<List<WeatherData>> callback) {
        executor.execute(() -> {
            try {
                String today = sdf.format(Calendar.getInstance().getTime());
                List<WeatherCacheEntity> entities = database.weatherCacheDao().getForecast(regionId, today);

                if (entities == null || entities.isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(null));
                    return;
                }

                List<WeatherData> result = new ArrayList<>();
                for (WeatherCacheEntity entity : entities) {
                    WeatherData wd = new WeatherData();
                    wd.setDate(entity.date);
                    wd.setTemperatureMin(entity.temperatureMin);
                    wd.setTemperatureMax(entity.temperatureMax);
                    wd.setHumidityMin(entity.humidityMin);
                    wd.setHumidityMax(entity.humidityMax);
                    wd.setWindMin(entity.windMin);
                    wd.setWindMax(entity.windMax);
                    wd.setPrecipitation(entity.precipitation);
                    wd.setPressure(entity.pressure);
                    result.add(wd);
                }

                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Error getting forecast: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void hasFreshCache(int regionId, CacheCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                String today = sdf.format(Calendar.getInstance().getTime());
                int count = database.weatherCacheDao().getCount(regionId, today);

                if (count == 0) {
                    mainHandler.post(() -> callback.onSuccess(false));
                    return;
                }

                List<WeatherCacheEntity> entities = database.weatherCacheDao().getForecast(regionId, today);
                if (entities == null || entities.isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(false));
                    return;
                }

                long ageLimit = System.currentTimeMillis() - CACHE_EXPIRY_MS;
                boolean isFresh = entities.get(0).cachedAt > ageLimit;
                mainHandler.post(() -> callback.onSuccess(isFresh));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void cleanOldCache(VoidCallback callback) {
        executor.execute(() -> {
            try {
                long expiryTime = System.currentTimeMillis() - CACHE_EXPIRY_MS;
                database.weatherCacheDao().deleteOld(expiryTime);
                Log.d(TAG, "Cleaned old cache entries older than " + 7 + " days");
                if (callback != null) {
                    mainHandler.post(callback::onSuccess);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning old cache: " + e.getMessage());
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }
}