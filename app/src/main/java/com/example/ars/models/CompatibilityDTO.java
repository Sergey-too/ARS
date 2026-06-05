package com.example.ars.models;

import com.google.gson.annotations.SerializedName;

public class CompatibilityDTO {
    @SerializedName("crop1")
    private String crop1;

    @SerializedName("crop2")
    private String crop2;

    @SerializedName("status")
    private Integer status;

    @SerializedName("cropId1")
    private Integer cropId1;

    @SerializedName("cropId2")
    private Integer cropId2;

    public CompatibilityDTO(String crop1, String crop2, Integer status) {
        this.crop1 = crop1;
        this.crop2 = crop2;
        this.status = status;
    }

    public CompatibilityDTO(String crop1, String crop2, Integer status, Integer cropId1, Integer cropId2) {
        this.crop1 = crop1;
        this.crop2 = crop2;
        this.status = status;
        this.cropId1 = cropId1;
        this.cropId2 = cropId2;
    }

    public String getCrop1() { return crop1; }
    public String getCrop2() { return crop2; }
    public Integer getStatus() { return status; }
    public Integer getCropId1() { return cropId1; }
    public Integer getCropId2() { return cropId2; }

    public void setStatus(Integer status) { this.status = status; }
    public void setCropId1(Integer cropId1) { this.cropId1 = cropId1; }
    public void setCropId2(Integer cropId2) { this.cropId2 = cropId2; }

    public String getCropName1() { return crop1; }
    public String getCropName2() { return crop2; }
}