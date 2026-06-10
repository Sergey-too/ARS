package com.example.ars.models;

public class DeleteResponse {
    private boolean success;
    private String message;
    private String error;
    private Integer deletedId;
    private String date;
    private String region;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public Integer getDeletedId() { return deletedId; }
    public void setDeletedId(Integer deletedId) { this.deletedId = deletedId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
}