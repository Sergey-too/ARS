package com.example.ars.models;

import com.google.gson.annotations.Expose;
import java.time.LocalDateTime;

public class SupportRequest {
    private Integer id;
    private Integer userId;
    private String subject;
    private String content;
    private Integer statusId;

    @Expose(serialize = false, deserialize = true)
    private String createdAt;

    public SupportRequest(Integer userId, String subject, String content) {
        this.userId = userId;
        this.subject = subject;
        this.content = content;
        this.statusId = 1;
    }
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getStatusId() { return statusId; }
    public void setStatusId(Integer statusId) { this.statusId = statusId; }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}