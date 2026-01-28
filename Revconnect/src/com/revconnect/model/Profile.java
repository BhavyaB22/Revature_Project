package com.revconnect.model;

public class Profile {
    private int userId;
    private String username, bio, location, picPath, website;
    private String businessName, category, address, contactInfo, businessHours;

    // Standard Constructor
    public Profile() {}

    // Getters and Setters (Required for ProfileDAO to work)
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPicPath() { return picPath; }
    public void setPicPath(String picPath) { this.picPath = picPath; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

    public String getBusinessHours() { return businessHours; }
    public void setBusinessHours(String businessHours) { this.businessHours = businessHours; }
}