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

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getActionTypeId() { return actionTypeId; }
    public void setActionTypeId(Integer actionTypeId) { this.actionTypeId = actionTypeId; }

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }

    public String getDoneAt() { return doneAt; }
    public void setDoneAt(String doneAt) { this.doneAt = doneAt; }

    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }
}