package com.example.ars.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class GardenHistory implements Serializable {
    private Integer id;

    @SerializedName("action_type_id")
    private Integer actionTypeId;

    @SerializedName("done_at")
    private String doneAt;

    @SerializedName("crop_name")
    private String cropName;

    private String variety;

    @SerializedName("area_name")
    private String areaName;

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