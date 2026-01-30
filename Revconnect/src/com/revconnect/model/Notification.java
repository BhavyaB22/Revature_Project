package com.revconnect.model;

public class Notification {
    // Private fields
    private int notifId;
    private int postId;
    private int receiverId;
    private int senderId;
    private String message;
    private String type;
    private String senderUsername;
    private int readStatus;

    // Default Constructor
    public Notification() {}

    // Getters and Setters
    public int getNotifId() {
        return notifId;
    }

    public void setNotifId(int notifId) {
        this.notifId = notifId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public int getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(int readStatus) {
        this.readStatus = readStatus;
    }
    public int getPostId() { return postId; } // Add this getter
    public void setPostId(int postId) { this.postId = postId; }

    // toString method for easy console printing/debugging
    @Override
    public String toString() {
        String status = (readStatus == 0) ? "[NEW]" : "[READ]";
        return status + " " + senderUsername + ": " + message;
    }
}