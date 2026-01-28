package com.revconnect.service;

import com.revconnect.model.Profile;

public class ProfileService {
    
    public boolean validateBasicProfile(Profile p) {
        // Validation for Personal/Creator (Username, Bio, Location required)
        return p.getUsername() != null && !p.getUsername().trim().isEmpty() &&
               p.getBio() != null && !p.getBio().trim().isEmpty() &&
               p.getLocation() != null && !p.getLocation().trim().isEmpty();
    }

    public boolean validateBusinessProfile(Profile p) {
        // Business requires all Basic + Category + Address
        return validateBasicProfile(p) && 
               p.getCategory() != null && p.getAddress() != null;
    }
}