package com.example.ars.models;

import com.google.gson.annotations.SerializedName;

public class UserCrop {
    private Integer id;

    @SerializedName("userId")
    private Integer userId;

    @SerializedName("cropId")
    private Integer cropId;

    @SerializedName("regionId")
    private Integer regionId;

    @SerializedName("crop")
    private Crop crop;

    @SerializedName("region")
    private Region region;

    // Геттеры и сеттеры
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getCropId() { return cropId; }
    public void setCropId(Integer cropId) { this.cropId = cropId; }

    public Integer getRegionId() { return regionId; }
    public void setRegionId(Integer regionId) { this.regionId = regionId; }

    public Crop getCrop() { return crop; }
    public void setCrop(Crop crop) { this.crop = crop; }

    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
}