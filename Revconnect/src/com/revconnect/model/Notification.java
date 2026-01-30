package com.revconnect.model;

public class Notification {
    private int notifId;      // Matches NOTIF_ID in DB
    private int postId;       // Matches POST_ID in DB
    private int receiverId;   // Matches USER_ID in DB
    private int senderId;     // Matches SENDER_ID in DB
    private String message;   // Matches MESSAGE in DB
    private String type;      // Matches NOTIF_TYPE in DB
    private String senderUsername;
    private int readStatus;   // Matches READ_STATUS in DB

    public Notification() {}

    // Primary ID Getter/Setter (Fixed naming for DAO)
    public int getNotifId() { return notifId; }
    public void setNotifId(int notifId) { this.notifId = notifId; }

    // Relationship Getters/Setters
    public int getReceiverId() { return receiverId; }
    public void setReceiverId(int receiverId) { this.receiverId = receiverId; }

    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }

    // Content Getters/Setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }

    // Status & Meta Getters/Setters
    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public int getReadStatus() { return readStatus; }
    public void setReadStatus(int readStatus) { this.readStatus = readStatus; }

    // Helper to identify if an action (Accept/Reject) is needed
    public boolean isActionable() {
        return "CONNECT".equalsIgnoreCase(this.type);
    }

    @Override
    public String toString() {
        String status = (readStatus == 0) ? "[NEW]" : "[READ]";
        return status + " " + (senderUsername != null ? senderUsername : "User") + ": " + message;
    }
}