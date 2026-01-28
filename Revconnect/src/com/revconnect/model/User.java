package com.revconnect.model;

public class User {
    private int userId;
    private String email, password, userType, privacy, sQuestion, sAnswer;

    public User() {}

    // Getters and Setters
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getPrivacy() { return privacy; }
    public void setPrivacy(String privacy) { this.privacy = privacy; }
    public String getsQuestion() { return sQuestion; }
    public void setsQuestion(String sQuestion) { this.sQuestion = sQuestion; }
    public String getsAnswer() { return sAnswer; }
    public void setsAnswer(String sAnswer) { this.sAnswer = sAnswer; }
}