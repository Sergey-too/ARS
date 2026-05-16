package com.example.ars.models;

import com.google.gson.annotations.SerializedName;

public class History {
    private int id;
    private int actionTypeId;
    private String doneAt;
    private String cropName;
    private String variety;
    private String areaName;
    private int regionId;
    private int wateringInterval;
    private int fertilizingInterval;
    private int soilCareInterval;
    private int protectionInterval;

    private String temperature;
    private String humidity;
    private String precipitation;

    private WeatherData weather;

    // Геттеры
    public String getDoneAt() { return doneAt; }
    public String getCropName() { return cropName; }
    public String getVariety() { return variety; }
    public String getAreaName() { return areaName; }
    public int getActionTypeId() { return actionTypeId; }
    public int getRegionId() { return regionId; }
    public WeatherData getWeather() { return weather; }
    public void setWeather(WeatherData weather) { this.weather = weather; }
    public int getWateringInterval() { return wateringInterval; }
    public int getFertilizingInterval() { return fertilizingInterval; }
    public int getSoilCareInterval() { return soilCareInterval; }

    public String getTemperature() { return temperature; }
    public void setTemperature(String temperature) { this.temperature = temperature; }

    public String getHumidity() { return humidity; }
    public void setHumidity(String humidity) { this.humidity = humidity; }

    public String getPrecipitation() { return precipitation; }
    public void setPrecipitation(String precipitation) { this.precipitation = precipitation; }

    public String getActionName() {
        switch (actionTypeId) {
            case 1: return "Посадка";
            case 2: return "Полив";
            case 3: return "Удобрение";
            case 4: return "Сбор урожая";
            case 5: return "Рыхление";
            case 6: return "Защита";
            default: return "Уход";
        }
    }
}

