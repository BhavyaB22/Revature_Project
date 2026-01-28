package com.revconnect.service;

import com.revconnect.model.User;
import com.revconnect.dao.UserDAO;

public class AuthService {
    private UserDAO userDAO = new UserDAO();

    public String validateAndRegister(User user) {
        if (userDAO.isEmailExists(user.getEmail())) {
            return "Error: User already exists with this email!";
        }
        String email = user.getEmail();
        // ^[a-zA-Z0-9]             -> Must start with letter/digit
        // (?!.*\\.\\.)             -> No consecutive dots
        // [a-zA-Z0-9._%+-]{0,62}   -> Middle chars (max 64 total before @)
        // [a-zA-Z0-9]              -> Must not end with special char
        // @                        -> Separator
        // [a-zA-Z0-9.-]+           -> Domain name letters/digits
        // \\.[a-zA-Z]{2,}$         -> Dot and TLD (length >= 2)
        String emailRegex = "^[a-zA-Z0-9](?!.*\\.\\.)[a-zA-Z0-9._%+-]{0,62}[a-zA-Z0-9]@([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$";

        if (email == null || !email.matches(emailRegex) || email.contains(" ")) {
            return "Invalid Email format! Ensure it follows all local and domain rules.";
        }

        // 3. Password Validation
        return validatePasswordRules(user.getPassword());
    }

    public String validatePasswordRules(String p) {
    	if (p != null && p.startsWith("-")) {
            return "Invalid input"; 
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
}