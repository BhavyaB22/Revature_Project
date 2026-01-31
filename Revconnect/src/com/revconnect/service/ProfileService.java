package com.revconnect.service;

import com.revconnect.dao.ProfileDAO;
import com.revconnect.model.Profile;
import java.util.regex.Pattern;

public class ProfileService {
    private ProfileDAO profileDAO = new ProfileDAO();

    // ---------- VALIDATION LOGIC ----------

    // Helper method for Indian Phone Number Validation
    public boolean isValidIndianMobile(String contact) {
        if (contact == null) return false;
        // Regex: Starts with 6-9, followed by exactly 9 digits (Total 10)
        return Pattern.matches("^[6-9]\\d{9}$", contact.trim());
    }

    public boolean validateBasicProfile(Profile p) {
        return p.getUsername() != null && !p.getUsername().trim().isEmpty() &&
               p.getBio() != null && !p.getBio().trim().isEmpty() &&
               p.getLocation() != null && !p.getLocation().trim().isEmpty();
    }

    public boolean validateBusinessProfile(Profile p, String contact) {
        // Business requires basic fields + Category + Address + Valid Phone
        return validateBasicProfile(p) && 
               p.getCategory() != null && !p.getCategory().trim().isEmpty() &&
               p.getAddress() != null && !p.getAddress().trim().isEmpty() &&
               isValidIndianMobile(contact);
    }

    // ---------- DATABASE LOGIC ----------

    public Profile getProfile(int userId) {
        return profileDAO.getProfileByUserId(userId);
    }

    public boolean updatePersonalProfile(int userId, String name, String bio, String loc, String web) {
        return profileDAO.updateProfile(userId, name, bio, loc, web);
    }

    public boolean updateEnhancedProfile(int userId, String name, String category, String bio, 
                                          String address, String contact, String web, String hours) {
        
        // Packing details to avoid changing the DB table structure
        String detailedBio = "Category: " + category + " | " + bio + " | Contact: " + contact;
        String detailedLoc = address + " (Hours: " + hours + ")";
        
        return profileDAO.updateProfile(userId, name, detailedBio, detailedLoc, web);
    }
}