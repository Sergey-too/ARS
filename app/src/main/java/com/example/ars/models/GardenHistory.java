package com.example.ars.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class GardenHistory implements Serializable {
    private Integer id;

    @SerializedName("actionTypeId")
    private Integer actionTypeId;

    @SerializedName("cropName")
    private String cropName;

    @SerializedName("areaName")
    private String areaName;

    @SerializedName("done_at")
    private String doneAt;

    private String variety;

    @SerializedName("watering_interval") private Integer wateringInterval;
    @SerializedName("fertilizing_interval") private Integer fertilizingInterval;
    @SerializedName("soil_care_interval") private Integer soilCareInterval;
    @SerializedName("protection_interval") private Integer protectionInterval;

    // Геттеры
    public Integer getActionTypeId() { return actionTypeId; }
    public String getCropName() { return cropName; }
    public String getVariety() { return variety; }
    public String getAreaName() { return areaName; }
    public Integer getWateringInterval() { return wateringInterval; }
    public Integer getFertilizingInterval() { return fertilizingInterval; }
}