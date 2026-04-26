package com.example.ars.models;

import java.io.Serializable;

public class IndividualUserCrop implements Serializable {
    private Integer id;
    private Integer userId;
    private String name;
    private String description;
    private Double minTemp;
    private Double maxTemp;
    private Double maxWind;
    private Double minHumidity;
    private Double maxHumidity;
    private Double neededPrecipitation;
    private Double sowingDepth;
    private Integer daysToGermination;
    private Integer daysToHarvest;
    private boolean canSeedlings;
    private boolean canDirectSow;
    private String localPhotoPath;
    private Integer categoryId;

    // Пустой конструктор
    public IndividualUserCrop() {}

    // Геттеры и сеттеры
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getMinTemp() { return minTemp; }
    public void setMinTemp(Double minTemp) { this.minTemp = minTemp; }

    public Double getMaxTemp() { return maxTemp; }
    public void setMaxTemp(Double maxTemp) { this.maxTemp = maxTemp; }

    public Double getMaxWind() { return maxWind; }
    public void setMaxWind(Double maxWind) { this.maxWind = maxWind; }

    public Double getMinHumidity() { return minHumidity; }
    public void setMinHumidity(Double minHumidity) { this.minHumidity = minHumidity; }

    public Double getMaxHumidity() { return maxHumidity; }
    public void setMaxHumidity(Double maxHumidity) { this.maxHumidity = maxHumidity; }

    public Double getNeededPrecipitation() { return neededPrecipitation; }
    public void setNeededPrecipitation(Double neededPrecipitation) { this.neededPrecipitation = neededPrecipitation; }

    public Double getSowingDepth() { return sowingDepth; }
    public void setSowingDepth(Double sowingDepth) { this.sowingDepth = sowingDepth; }

    public Integer getDaysToGermination() { return daysToGermination; }
    public void setDaysToGermination(Integer daysToGermination) { this.daysToGermination = daysToGermination; }

    public Integer getDaysToHarvest() { return daysToHarvest; }
    public void setDaysToHarvest(Integer daysToHarvest) { this.daysToHarvest = daysToHarvest; }

    public boolean isCanSeedlings() { return canSeedlings; }
    public void setCanSeedlings(boolean canSeedlings) { this.canSeedlings = canSeedlings; }

    public boolean isCanDirectSow() { return canDirectSow; }
    public void setCanDirectSow(boolean canDirectSow) { this.canDirectSow = canDirectSow; }

    public String getLocalPhotoPath() { return localPhotoPath; }
    public void setLocalPhotoPath(String localPhotoPath) { this.localPhotoPath = localPhotoPath; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
}