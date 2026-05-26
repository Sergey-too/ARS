package com.example.ars.models;

public class TaskItem {
    private String cropName;
    private String variety;
    private String areaName;
    private Integer actionTypeId;
    private String actionName;
    private String dueDate;
    private Boolean isOverdue;

    private String lastDoneAt;

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }

    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }

    public Integer getActionTypeId() { return actionTypeId; }
    public void setActionTypeId(Integer actionTypeId) { this.actionTypeId = actionTypeId; }

    public String getActionName() { return actionName; }
    public void setActionName(String actionName) { this.actionName = actionName; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public Boolean getIsOverdue() { return isOverdue; }
    public void setIsOverdue(Boolean isOverdue) { this.isOverdue = isOverdue; }
    public String getLastDoneAt() { return lastDoneAt; }
    public void setLastDoneAt(String lastDoneAt) { this.lastDoneAt = lastDoneAt; }

    public String getDisplayName() {
        return (variety != null && !variety.isEmpty()) ? cropName + " (" + variety + ")" : cropName;
    }
}