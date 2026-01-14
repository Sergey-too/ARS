package com.example.ars.models;

import java.util.List;
import java.util.Map;

public class WeatherResponse {
    private String region;
    private List<WeatherData> weather;
    private boolean isTestData;
    private String message;
    private int regionsCount;

    // Геттеры и сеттеры
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public List<WeatherData> getWeather() { return weather; }
    public void setWeather(List<WeatherData> weather) { this.weather = weather; }

    public boolean isTestData() { return isTestData; }
    public void setTestData(boolean testData) { isTestData = testData; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getRegionsCount() { return regionsCount; }
    public void setRegionsCount(int regionsCount) { this.regionsCount = regionsCount; }
}