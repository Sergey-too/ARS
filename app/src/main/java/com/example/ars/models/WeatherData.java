package com.example.ars.models;

import com.google.gson.annotations.SerializedName;

public class WeatherData {
    private String date;

    @SerializedName("temperatureMin")
    private Double tempMin;

    @SerializedName("temperatureMax")
    private Double tempMax;

    @SerializedName("humidityMin")
    private Double humMin;

    @SerializedName("humidityMax")
    private Double humMax;

    @SerializedName("windMin")
    private Double windMin;

    @SerializedName("windMax")
    private Double windMax;

    private Double precipitation;
    private String pressure;

    // Геттеры
    public String getDate() { return date; }
    public Double getTempMin() { return tempMin != null ? tempMin : 0.0; }
    public Double getTempMax() { return tempMax != null ? tempMax : 0.0; }
    public Double getHumMin() { return humMin != null ? humMin : 0.0; }
    public Double getHumMax() { return humMax != null ? humMax : 0.0; }
    public Double getWindMin() { return windMin != null ? windMin : 0.0; }
    public Double getWindMax() { return windMax != null ? windMax : 0.0; }
    public Double getPrecipitation() { return precipitation != null ? precipitation : 0.0; }
    public String getPressure() { return pressure; }
}