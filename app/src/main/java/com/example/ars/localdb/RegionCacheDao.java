package com.example.ars.localdb;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface RegionCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RegionCacheEntity> regions);

    @Query("SELECT * FROM regions ORDER BY name ASC")
    List<RegionCacheEntity> getAllRegions();

    @Query("SELECT COUNT(*) FROM regions")
    int getCount();

    @Query("DELETE FROM regions")
    void deleteAll();
}