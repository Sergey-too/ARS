package com.example.ars.models;

public class WeatherComparisonDTO {
    private String monthName;
    private Double avgFactTemp;
    private Double normalTemp;
    private Double avgFactHumidity;
    private Double normalHumidity;

    public String getMonthName() { return monthName; }
    public Double getAvgFactTemp() { return avgFactTemp; }
    public Double getNormalTemp() { return normalTemp; }
    public Double getAvgFactHumidity() { return avgFactHumidity; }
    public Double getNormalHumidity() { return normalHumidity; }
}
