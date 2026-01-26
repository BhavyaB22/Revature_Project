package com.revconnect.model;

public class User {
    private String email;
    private String password;
    private String userType;

    // Constructor for Registration
    public User(String email, String password, String userType) {
        this.email = email;
        this.password = password;
        this.userType = userType;
    }

    // Getters - These fix the "Undefined" errors in your DAO
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getUserType() { return userType; }
    
    // Setters
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setUserType(String userType) { this.userType = userType; }
}