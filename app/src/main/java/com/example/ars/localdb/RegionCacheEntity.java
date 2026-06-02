package com.example.ars.localdb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "regions")
public class RegionCacheEntity {
    @PrimaryKey
    public int id;
    public String name;
    public long cachedAt;

    public RegionCacheEntity() {}

    public RegionCacheEntity(int id, String name, long cachedAt) {
        this.id = id;
        this.name = name;
        this.cachedAt = cachedAt;
    }
}