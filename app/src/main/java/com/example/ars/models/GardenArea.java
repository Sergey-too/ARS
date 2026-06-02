package com.example.ars.models;

import java.io.Serializable;

public class GardenArea implements Serializable {
    private Integer gardenId;
    private Integer areaId;

    public GardenArea() {}

    public Integer getGardenId() { return gardenId; }
    public void setGardenId(Integer gardenId) { this.gardenId = gardenId; }

    public Integer getAreaId() { return areaId; }
    public void setAreaId(Integer areaId) { this.areaId = areaId; }
}