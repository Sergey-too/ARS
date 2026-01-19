package com.example.ars.models;

public class PlantingRecommendation {
    private String date;
    private String cropName;
    private String regionName;
    private String reason;
    private String weatherTemperature;
    private String weatherHumidity;
    private String weatherWind;
    private boolean goodDay;

    // Геттеры и сеттеры
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

    public String getRegionName() { return regionName; }
    public void setRegionName(String regionName) { this.regionName = regionName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getWeatherTemperature() {
        return weatherTemperature;
    }

    public void setWeatherTemperature(String weatherTemperature) {
        this.weatherTemperature = weatherTemperature;
    }

    public String getWeatherHumidity() {
        return weatherHumidity;
    }

    public void setWeatherHumidity(String weatherHumidity) {
        this.weatherHumidity = weatherHumidity;
    }

    public String getWeatherWind() {
        return weatherWind;
    }

    public void setWeatherWind(String weatherWind) {
        this.weatherWind = weatherWind;
    }

    public boolean isGoodDay() {
        return goodDay;
    }

    public void setGoodDay(boolean goodDay) {
        this.goodDay = goodDay;
    }
}