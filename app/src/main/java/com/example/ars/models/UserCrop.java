package com.example.ars.models;

import com.google.gson.annotations.SerializedName;

public class UserCrop {
    private Integer id;

    @SerializedName("userId")
    private Integer userId;

    @SerializedName("cropId")
    private Integer cropId;

    @SerializedName("crop")
    private Crop crop;

    // Геттеры и сеттеры
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getCropId() { return cropId; }
    public void setCropId(Integer cropId) { this.cropId = cropId; }

    public Crop getCrop() { return crop; }
    public void setCrop(Crop crop) { this.crop = crop; }
}