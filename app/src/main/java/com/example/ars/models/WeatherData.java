package com.example.ars.models;


public class WeatherData {
    private String date;
    private String temperature;
    private String wind;
    private String pressure;
    private String humidity;
    private String precipitation;
    private String condition;

    // Геттеры и сеттеры
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTemperature() { return temperature; }
    public void setTemperature(String temperature) { this.temperature = temperature; }

    public String getWind() { return wind; }
    public void setWind(String wind) { this.wind = wind; }

    public String getPressure() { return pressure; }
    public void setPressure(String pressure) { this.pressure = pressure; }

    public String getHumidity() { return humidity; }
    public void setHumidity(String humidity) { this.humidity = humidity; }

    public String getPrecipitation() { return precipitation; }
    public void setPrecipitation(String precipitation) { this.precipitation = precipitation; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
}
