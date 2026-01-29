package com.revconnect.service;

import com.revconnect.model.User;
import com.revconnect.dao.UserDAO;

public class AuthService {
    private UserDAO userDAO = new UserDAO();

    public String validateEmail(String email) {
        // Breakdown:
        // ^[a-zA-Z0-9]      -> Must start with a Letter or Number
        // [a-zA-Z0-9._%+-]* -> Can then contain dots, hyphens, etc.
        // @gmail\\.com$     -> Must end exactly with @gmail.com
    	String emailRegex = "^(?!.*\\.\\.)[a-zA-Z0-9][a-zA-Z0-9._%+-]*[a-zA-Z0-9]@gmail\\.com$";

        if (email == null || !email.matches(emailRegex)) {
            return "Invalid Email format! Must start with a letter/number and end with @gmail.com.";
        }
        return "SUCCESS";
    }
    public String validatePasswordRules(String p) {
        if (p == null) return "Password cannot be null.";

        // Updated regex: Allows hyphens, requires 1 Upper, 1 Lower, 1 Digit, 1 Special, Min 6 chars
        String passwordRegex = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&-]{6,}$";

        if (!p.matches(passwordRegex)) {
            return "Invalid! Password must be at least 6 chars and include: 1 Upper, 1 Lower, 1 Digit, and 1 Special char.";
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

        // Regex: Starts with 6-9, followed by 9 digits
        String regex = "^[6-9]\\d{9}$";

        if (!phone.matches(regex)) {
            return "Invalid! Must be a 10-digit number starting with 6-9.";
        }

        // Rule: Cannot be 10 of the same digits
        if (phone.matches("(\\d)\\1{9}")) {
            return "Invalid! Phone number cannot be all the same digits.";
        }

        return "SUCCESS";
    }
}