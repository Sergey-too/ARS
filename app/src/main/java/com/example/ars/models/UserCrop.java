package com.example.ars.models;

import com.google.gson.annotations.SerializedName;

public class UserCrop {
    private Integer id;

    @SerializedName("userId")
    private Integer userId;

    private Integer individualCropId;
    @SerializedName("cropId")
    private Integer cropId;

    // МЕНЯЕМ regionId на areaId
    @SerializedName("areaId")
    private Integer areaId;

    @SerializedName("crop")
    private Crop crop;
    @SerializedName("area")
    private Area area;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getCropId() { return cropId; }
    public void setCropId(Integer cropId) { this.cropId = cropId; }

    public Integer getAreaId() { return areaId; }
    public void setAreaId(Integer areaId) { this.areaId = areaId; }

    public Crop getCrop() { return crop; }
    public void setCrop(Crop crop) { this.crop = crop; }

    public Area getArea() { return area; }
    public void setArea(Area area) { this.area = area; }
}