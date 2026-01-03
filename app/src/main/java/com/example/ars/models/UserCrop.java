package com.example.ars.models;

import java.util.Date;

public class UserCrop {
    private Integer id;
    private Integer userId;
    private Integer cropId;
    private Date addedDate;
    private String description;
    private Crop crop; // Для связи с растением

    // Геттеры и сеттеры
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getCropId() { return cropId; }
    public void setCropId(Integer cropId) { this.cropId = cropId; }

    public Date getAddedDate() { return addedDate; }
    public void setAddedDate(Date addedDate) { this.addedDate = addedDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Crop getCrop() { return crop; }
    public void setCrop(Crop crop) { this.crop = crop; }
}