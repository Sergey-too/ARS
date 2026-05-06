package com.example.ars.models;

public class PlantingRecommendation {
    private String date;
    private String dayOfWeek;
    private String cropName;
    private String areaName;
    private String weatherText;
    private String reason;
    private boolean goodDay;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }
    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }
    public String getWeatherText() { return weatherText; }
    public void setWeatherText(String weatherText) { this.weatherText = weatherText; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public boolean isGoodDay() { return goodDay; }
    public void setGoodDay(boolean goodDay) { this.goodDay = goodDay; }
}