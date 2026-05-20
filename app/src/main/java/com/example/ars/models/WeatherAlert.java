package com.example.ars.models;

import com.google.gson.annotations.SerializedName;

public class WeatherAlert {
    @SerializedName("id")
    private Integer id;

    @SerializedName("regionId")
    private Integer regionId;

    @SerializedName("alertText")
    private String alertText;

    @SerializedName("alertDate")
    private String alertDate;

    public Integer getId() { return id; }
    public Integer getRegionId() { return regionId; }
    public String getAlertText() { return alertText; }
    public String getAlertDate() { return alertDate; }
}