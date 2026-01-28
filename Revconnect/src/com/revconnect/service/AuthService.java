package com.revconnect.service;

import com.revconnect.model.User;
import com.revconnect.dao.UserDAO;

public class AuthService {
    private UserDAO userDAO = new UserDAO();

    public String validateEmail(String email) {
        // Corrected Regex:
    	// Strict Regex for Gmail only:
    	// ^[a-zA-Z0-9]             -> Starts with letter or digit
    	// [a-zA-Z0-9._%+-]* -> Middle characters
    	// @gmail\\.com$            -> Must end exactly with @gmail.com
    	String emailRegex = "^[a-zA-Z0-9][a-zA-Z0-9._%+-]*@gmail\\.com$";

        if (email == null || !email.matches(emailRegex)) {
            return "Invalid Email format! Ensure it starts with a letter/digit and has a valid domain (e.g. .com).";
        }
        return "SUCCESS";
    }

    public String validatePasswordRules(String p) {
    	
    	if (p.matches(".*-\\d.*")) { 
            return "Invalid input: Negative values  are not allowed anywhere.";
        }
    	if (p == null || p.length() < 6 ) { // Minimum 6 chars
            return "Invalid! Password must be at least 6 characters.";
        }
        
        boolean hasUpper = !p.equals(p.toLowerCase());
        boolean hasLower = !p.equals(p.toUpperCase());
        boolean hasSpecial = p.matches(".*[!@#$%^&*()].*");
        boolean hasDigit = p.matches(".*\\d.*"); // Check for numeric
        
        if (!hasUpper || !hasLower || !hasSpecial || !hasDigit) {
            return "Invalid! Password must have: 1 Upper, 1 Lower, 1 Numeric, and 1 Special char.";
        }
        return "SUCCESS";
    }

    public String recoverPassword(int userId, String newPass) {
        String validation = validatePasswordRules(newPass);
        if (!validation.equals("SUCCESS")) return validation;

        String oldPass = userDAO.getOldPassword(userId);
        if (newPass.equals(oldPass)) {
            return "Error: Previous password cannot be reused!";
        }

        return userDAO.updatePassword(userId, newPass) ? "SUCCESS" : "Update failed.";
    }
    public String validatePhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "Phone number cannot be empty.";
        }

        // Rule: No leading or trailing spaces
        if (!phone.equals(phone.trim())) {
            return "Invalid! Phone number cannot have leading or trailing spaces.";
        }

        /* Regex Breakdown:
           ^(\(\+91\))?  -> Optional (+91) at the start
           [6-9]         -> Must start with 6, 7, 8, or 9
           \d{9}         -> Followed by exactly 9 more digits (Total 10 digits)
           $             -> End of string
        */
        String regex = "^(\\(\\+91\\))?[6-9]\\d{9}$";

        // Rule: Digits only (after optional country code), Length, and Starting digits
        if (!phone.matches(regex)) {
            return "Invalid Format! Use 10 digits starting with 6-9 (e.g., 9876543210) " +
                   "or include country code like (+91)9876543210. No spaces or hyphens.";
        }

        // Rule: Cannot be all zeros or a repeated single digit
        if (phone.matches(".*(\\d)\\1{9}.*")) {
            return "Invalid! Phone number cannot consist of the same repeated digit.";
        }

        return "SUCCESS";
    }
}