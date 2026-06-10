package com.example.ars.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class UserCrop implements Serializable {
    private Integer id;
    private Integer userId;
    private Integer cropId;
    private Integer individualCropId;
    private Integer areaId;
    private Integer gardenId;
    private String status;

    @SerializedName("plantedAt")
    private String plantedAt;

    @SerializedName("harvestedAt")
    private String harvestedAt;

    private Crop crop;
    private IndividualUserCrop individualCrop;
    private Area area;
    private Garden garden;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getCropId() { return cropId; }
    public void setCropId(Integer cropId) { this.cropId = cropId; }

    public Integer getIndividualCropId() { return individualCropId; }
    public void setIndividualCropId(Integer individualCropId) { this.individualCropId = individualCropId; }

    public Integer getAreaId() { return areaId; }
    public void setAreaId(Integer areaId) { this.areaId = areaId; }

    public Integer getGardenId() { return gardenId; }
    public void setGardenId(Integer gardenId) { this.gardenId = gardenId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPlantedAt() { return plantedAt; }
    public void setPlantedAt(String plantedAt) { this.plantedAt = plantedAt; }

    public String getHarvestedAt() { return harvestedAt; }
    public void setHarvestedAt(String harvestedAt) { this.harvestedAt = harvestedAt; }

    public Crop getCrop() { return crop; }
    public void setCrop(Crop crop) { this.crop = crop; }

    public IndividualUserCrop getIndividualCrop() { return individualCrop; }
    public void setIndividualCrop(IndividualUserCrop individualCrop) { this.individualCrop = individualCrop; }

    public Area getArea() { return area; }
    public void setArea(Area area) { this.area = area; }

    public Garden getGarden() { return garden; }
    public void setGarden(Garden garden) { this.garden = garden; }

    public String getName() {
        if (crop != null) return crop.getName();
        if (individualCrop != null) return individualCrop.getName();
        return "Без названия";
    }
}