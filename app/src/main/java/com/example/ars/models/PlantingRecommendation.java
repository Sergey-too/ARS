package com.example.ars.models;

public class PlantingRecommendation {
    private String date;
    private String dayOfWeek;
    private String cropName;
    private String variety;
    private String areaName;
    private String gardenName;
    private Integer cropId;
    private Integer areaId;
    private Integer userCropId;
    private String weatherText;
    private String reason;
    private boolean goodDay;
    private String tempCurrent;
    private String tempRequired;
    private String humidityCurrent;
    private String humidityRequired;
    private String precipCurrent;
    private String precipRequired;
    private String windCurrent;
    private String windRequired;

    // Getters and Setters
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }

    public String getGardenName() { return gardenName; }
    public void setGardenName(String gardenName) { this.gardenName = gardenName; }

    public Integer getCropId() { return cropId; }
    public void setCropId(Integer cropId) { this.cropId = cropId; }

    public Integer getAreaId() { return areaId; }
    public void setAreaId(Integer areaId) { this.areaId = areaId; }

    public Integer getUserCropId() { return userCropId; }
    public void setUserCropId(Integer userCropId) { this.userCropId = userCropId; }

    public String getWeatherText() { return weatherText; }
    public void setWeatherText(String weatherText) { this.weatherText = weatherText; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public boolean isGoodDay() { return goodDay; }
    public void setGoodDay(boolean goodDay) { this.goodDay = goodDay; }

    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }

    public String getTempCurrent() { return tempCurrent; }
    public void setTempCurrent(String tempCurrent) { this.tempCurrent = tempCurrent; }

    public String getTempRequired() { return tempRequired; }
    public void setTempRequired(String tempRequired) { this.tempRequired = tempRequired; }

    public String getHumidityCurrent() { return humidityCurrent; }
    public void setHumidityCurrent(String humidityCurrent) { this.humidityCurrent = humidityCurrent; }

    public String getHumidityRequired() { return humidityRequired; }
    public void setHumidityRequired(String humidityRequired) { this.humidityRequired = humidityRequired; }

    public String getPrecipCurrent() { return precipCurrent; }
    public void setPrecipCurrent(String precipCurrent) { this.precipCurrent = precipCurrent; }

    public String getPrecipRequired() { return precipRequired; }
    public void setPrecipRequired(String precipRequired) { this.precipRequired = precipRequired; }

    public String getWindCurrent() { return windCurrent; }
    public void setWindCurrent(String windCurrent) { this.windCurrent = windCurrent; }

    public String getWindRequired() { return windRequired; }
    public void setWindRequired(String windRequired) { this.windRequired = windRequired; }
}