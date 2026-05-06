package com.example.ars.models;

import com.google.gson.annotations.SerializedName;

public class WeatherData {

    @SerializedName("id")
    private Integer id;

    @SerializedName("regionId")
    private Integer regionId;

    @SerializedName("date")
    private String date;

    @SerializedName("temperatureMin")
    private String temperatureMin;

    @SerializedName("temperatureMax")
    private String temperatureMax;

    @SerializedName("humidityMin")
    private String humidityMin;

    @SerializedName("humidityMax")
    private String humidityMax;

    @SerializedName("windMin")
    private String windMin;

    @SerializedName("windMax")
    private String windMax;

    @SerializedName("precipitation")
    private String precipitation;

    @SerializedName("pressure")
    private String pressure;

    @SerializedName("gustsOfWind")
    private String gustsOfWind;

    // Геттеры
    public Integer getId() { return id; }
    public Integer getRegionId() { return regionId; }
    public String getDate() { return date; }
    public String getTemperatureMin() { return temperatureMin != null ? temperatureMin : "0"; }
    public String getTemperatureMax() { return temperatureMax != null ? temperatureMax : "0"; }
    public String getHumidityMin() { return humidityMin != null ? humidityMin : "0"; }
    public String getHumidityMax() { return humidityMax != null ? humidityMax : "0"; }
    public String getWindMin() { return windMin != null ? windMin : "0"; }
    public String getWindMax() { return windMax != null ? windMax : "0"; }
    public String getPrecipitation() { return precipitation != null ? precipitation : "0"; }
    public String getPressure() { return pressure != null ? pressure : "--"; }
    public String getGustsOfWind() { return gustsOfWind; }

    // Сеттеры (если нужны)
    public void setId(Integer id) { this.id = id; }
    public void setRegionId(Integer regionId) { this.regionId = regionId; }
    public void setDate(String date) { this.date = date; }
    public void setTemperatureMin(String temperatureMin) { this.temperatureMin = temperatureMin; }
    public void setTemperatureMax(String temperatureMax) { this.temperatureMax = temperatureMax; }
    public void setHumidityMin(String humidityMin) { this.humidityMin = humidityMin; }
    public void setHumidityMax(String humidityMax) { this.humidityMax = humidityMax; }
    public void setWindMin(String windMin) { this.windMin = windMin; }
    public void setWindMax(String windMax) { this.windMax = windMax; }
    public void setPrecipitation(String precipitation) { this.precipitation = precipitation; }
    public void setPressure(String pressure) { this.pressure = pressure; }
    public void setGustsOfWind(String gustsOfWind) { this.gustsOfWind = gustsOfWind; }

    // Форматированные строки для отображения
    public String getTempRange() {
        return temperatureMin + "° - " + temperatureMax + "°";
    }

    public String getHumidityRange() {
        return humidityMin + "% - " + humidityMax + "%";
    }

    public String getWindRange() {
        return windMin + " - " + windMax + " м/с";
    }

    public String getWindPrecipText() {
        return precipitation + " мм" + windMax + " м/с";
    }

    public String getPressureText() {
        return "давление: " + pressure + " мм рт.ст.";
    }
}