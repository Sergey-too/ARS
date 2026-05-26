package com.example.ars.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Crop implements Serializable {
    private Integer id;
    private String name;
    private String variety;
    private String category;
    private String description;

    @SerializedName("minTemp") private Short minTemp;      // smallint → Short
    @SerializedName("maxTemp") private Short maxTemp;      // smallint → Short
    @SerializedName("maxWind") private Short maxWind;      // smallint → Short
    @SerializedName("minHumidity") private Integer minHumidity;  // smallint → Integer
    @SerializedName("maxHumidity") private Integer maxHumidity;  // smallint → Integer
    @SerializedName("neededPrecipitation") private Short neededPrecipitation;  // smallint → Short

    @SerializedName("sowingDepth") private Integer sowingDepth;
    @SerializedName("daysToGermination") private Integer daysToGermination;
    @SerializedName("daysToHarvest") private Integer daysToHarvest;
    @SerializedName("canSeedlings") private Boolean canSeedlings;
    @SerializedName("canDirectSow") private Boolean canDirectSow;
    @SerializedName("photoPath") private String photoPath;

    @SerializedName("wateringInterval") private Integer wateringInterval;
    @SerializedName("fertilizingInterval") private Integer fertilizingInterval;
    @SerializedName("soilCareInterval") private Integer soilCareInterval;
    @SerializedName("protectionInterval") private Integer protectionInterval;

    // Геттеры и сеттеры
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Short getMinTemp() { return minTemp; }
    public void setMinTemp(Short minTemp) { this.minTemp = minTemp; }

    public Short getMaxTemp() { return maxTemp; }
    public void setMaxTemp(Short maxTemp) { this.maxTemp = maxTemp; }

    public Short getMaxWind() { return maxWind; }
    public void setMaxWind(Short maxWind) { this.maxWind = maxWind; }

    public Integer getMinHumidity() { return minHumidity; }
    public void setMinHumidity(Integer minHumidity) { this.minHumidity = minHumidity; }

    public Integer getMaxHumidity() { return maxHumidity; }
    public void setMaxHumidity(Integer maxHumidity) { this.maxHumidity = maxHumidity; }

    public Short getNeededPrecipitation() { return neededPrecipitation; }
    public void setNeededPrecipitation(Short neededPrecipitation) { this.neededPrecipitation = neededPrecipitation; }

    public Integer getSowingDepth() { return sowingDepth; }
    public void setSowingDepth(Integer sowingDepth) { this.sowingDepth = sowingDepth; }
    public Integer getDaysToGermination() { return daysToGermination; }
    public void setDaysToGermination(Integer daysToGermination) { this.daysToGermination = daysToGermination; }
    public Integer getDaysToHarvest() { return daysToHarvest; }
    public void setDaysToHarvest(Integer daysToHarvest) { this.daysToHarvest = daysToHarvest; }
    public Boolean getCanSeedlings() { return canSeedlings; }
    public void setCanSeedlings(Boolean canSeedlings) { this.canSeedlings = canSeedlings; }
    public Boolean getCanDirectSow() { return canDirectSow; }
    public void setCanDirectSow(Boolean canDirectSow) { this.canDirectSow = canDirectSow; }
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    public Integer getWateringInterval() { return wateringInterval; }
    public void setWateringInterval(Integer wateringInterval) { this.wateringInterval = wateringInterval; }
    public Integer getFertilizingInterval() { return fertilizingInterval; }
    public void setFertilizingInterval(Integer fertilizingInterval) { this.fertilizingInterval = fertilizingInterval; }
    public Integer getSoilCareInterval() { return soilCareInterval; }
    public void setSoilCareInterval(Integer soilCareInterval) { this.soilCareInterval = soilCareInterval; }
    public Integer getProtectionInterval() { return protectionInterval; }
    public void setProtectionInterval(Integer protectionInterval) { this.protectionInterval = protectionInterval; }
}