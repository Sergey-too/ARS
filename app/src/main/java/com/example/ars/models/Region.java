package com.example.ars.models;

// Region.java (Android)
public class Region {
    private Long id;
    private String name;

    public Region(String name) {
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return name;
    }
}
