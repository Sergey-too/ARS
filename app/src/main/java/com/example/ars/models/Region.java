package com.example.ars.models;

import com.google.gson.annotations.SerializedName;

public class Region {
    private Integer id;
    @SerializedName("name")
    private String name;

    public Region(String name) {
        this.name = name;

    }
    public Region() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return name;
    }
}
