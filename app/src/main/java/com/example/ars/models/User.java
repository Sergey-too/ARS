// User.java (Android)
package com.example.ars.models;

import com.google.gson.annotations.SerializedName;

public class User {
    private Integer id;
    private String name;
    private String email;
    private String login;
    private String password;
    private String createdAt;

    @SerializedName("registration_date")
    private String registrationDate;

    @SerializedName("is_admin")
    private boolean isAdmin;

    @SerializedName("in_ban")
    private boolean inBan;

    // Конструкторы
    public User() {}

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    // Геттеры и сеттеры
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean getIsAdmin() { return isAdmin; }
    public void setIsAdmin(boolean admin) { isAdmin = admin; }

    public boolean getInBan() { return inBan; }
    public void setInBan(boolean ban) { inBan = ban; }

    public String getRegistrationDate() { return createdAt; }
}