package com.example.ars.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class UserCategory implements Serializable {
    private Integer id;
    private Integer userId;
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("createdAt")
    private String createdAt;

    public UserCategory() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return name;
    }
}