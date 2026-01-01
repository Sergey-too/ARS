package com.example.ars.models;


import java.util.List;

// WeatherResponse.java (Android)
public class WeatherResponse {
    private String region;
    private List<WeatherData> weather;
    private boolean isTestData;

    // Геттеры и сеттеры
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public List<WeatherData> getWeather() { return weather; }
    public void setWeather(List<WeatherData> weather) { this.weather = weather; }

    public boolean isTestData() { return isTestData; }
    public void setTestData(boolean testData) { isTestData = testData; }
}

