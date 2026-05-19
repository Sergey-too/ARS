package com.example.ars.models;

public class SupportMessage {
    private Integer id;
    private Integer requestId;
    private Integer senderId;
    private String messageText;
    private String createdAt;

    public SupportMessage(Integer requestId, Integer senderId, String messageText) {
        this.requestId = requestId;
        this.senderId = senderId;
        this.messageText = messageText;
    }

    public Integer getId() { return id; }
    public Integer getRequestId() { return requestId; }
    public Integer getSenderId() { return senderId; }
    public String getMessageText() { return messageText; }
    public String getCreatedAt() { return createdAt; }
}