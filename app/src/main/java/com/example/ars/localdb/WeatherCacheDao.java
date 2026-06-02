package com.example.ars.localdb;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WeatherCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<WeatherCacheEntity> entities);

    @Query("SELECT * FROM weather_cache WHERE regionId = :regionId AND date >= :startDate ORDER BY date ASC")
    List<WeatherCacheEntity> getForecast(int regionId, String startDate);

    @Query("DELETE FROM weather_cache WHERE regionId = :regionId")
    void deleteByRegion(int regionId);

    @Query("DELETE FROM weather_cache WHERE cachedAt < :oldTimestamp")
    void deleteOld(long oldTimestamp);

    @Query("SELECT COUNT(*) FROM weather_cache WHERE regionId = :regionId AND date >= :startDate")
    int getCount(int regionId, String startDate);
}