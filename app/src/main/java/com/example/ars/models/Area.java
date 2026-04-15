package com.example.ars.models;

public class Area {
    private Integer id;
    private String name;
    private Integer regionId;
    private Region region;

    public Integer getId() { return id; }
    public String getName() { return name; }

    public Integer getRegionId() { return regionId; }

    public Region getRegion() { return region; }

    public Integer getSafeRegionId() {
        if (regionId != null) return regionId;
        if (region != null) return Math.toIntExact(region.getId());
        return null;
    }

    @Override
    public String toString() { return name; }
}