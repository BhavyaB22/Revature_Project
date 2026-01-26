package com.revconnect.model;

public class Profile {
    private int profileId;
    private int userId; // Foreign key to the User table
    private String fullName;
    private String bio;
    private String location;
    private String interests; // For Personal users
    private String businessCategory; // For Business users
    private String creatorNiche; // For Content Creators

    // Default Constructor
    public Profile() {}

    // Constructor for Profile Creation
    public Profile(int userId, String fullName, String bio, String location) {
        this.userId = userId;
        this.fullName = fullName;
        this.bio = bio;
        this.location = location;
    }

    // Getters and Setters
    public int getProfileId() { return profileId; }
    public void setProfileId(int profileId) { this.profileId = profileId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }

    public String getBusinessCategory() { return businessCategory; }
    public void setBusinessCategory(String businessCategory) { this.businessCategory = businessCategory; }

    public String getCreatorNiche() { return creatorNiche; }
    public void setCreatorNiche(String creatorNiche) { this.creatorNiche = creatorNiche; }
}