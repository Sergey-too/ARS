package com.example.ars.models;

public class ActionType {
    private Integer id;
    private String name;

    public ActionType() {}

    public ActionType(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
}