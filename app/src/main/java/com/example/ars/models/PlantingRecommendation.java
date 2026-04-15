package com.example.ars.models;

public class PlantingRecommendation {
    private String date;
    private String cropName;
    private String regionName;
    private String reason;
    private boolean goodDay;

    private Double tempMin;
    private Double tempMax;
    private Double humMin;
    private Double humMax;
    private Double windMax;

    // Геттеры и сеттеры
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }
    public String getRegionName() { return regionName; }
    public void setRegionName(String regionName) { this.regionName = regionName; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public boolean isGoodDay() { return goodDay; }
    public void setGoodDay(boolean goodDay) { this.goodDay = goodDay; }
    public Double getTempMin() { return tempMin; }
    public void setTempMin(Double tempMin) { this.tempMin = tempMin; }
    public Double getTempMax() { return tempMax; }
    public void setTempMax(Double tempMax) { this.tempMax = tempMax; }
    public Double getHumMin() { return humMin; }
    public void setHumMin(Double humMin) { this.humMin = humMin; }
    public Double getHumMax() { return humMax; }
    public void setHumMax(Double humMax) { this.humMax = humMax; }
    public Double getWindMax() { return windMax; }
    public void setWindMax(Double windMax) { this.windMax = windMax; }
}