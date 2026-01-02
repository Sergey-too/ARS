// AuthResponse.java
package com.example.ars.models;

public class AuthResponse {
    private boolean success;
    private String message;
    private String token;
    private User user;
    private String error;

    // Геттеры и сеттеры
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}