package com.example.ars.localdb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.ars.models.WeatherData;

@Entity(tableName = "weather_cache")
public class WeatherCacheEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public int regionId;
    public String date;
    public String temperatureMin;
    public String temperatureMax;
    public String humidityMin;
    public String humidityMax;
    public String windMin;
    public String windMax;
    public String precipitation;
    public String pressure;
    public long cachedAt;

    public WeatherCacheEntity() {}

    public WeatherCacheEntity(int regionId, WeatherData wd, long cachedAt) {
        this.regionId = regionId;
        this.date = wd.getDate();
        this.temperatureMin = wd.getTemperatureMin();
        this.temperatureMax = wd.getTemperatureMax();
        this.humidityMin = wd.getHumidityMin();
        this.humidityMax = wd.getHumidityMax();
        this.windMin = wd.getWindMin();
        this.windMax = wd.getWindMax();
        this.precipitation = wd.getPrecipitation();
        this.pressure = wd.getPressure();
        this.cachedAt = cachedAt;
    }
}