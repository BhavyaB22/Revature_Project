package com.revconnect.service;

public class AuthService {

    // ---------- EMAIL VALIDATION ----------
    public String validateEmail(String email) {

        if (email == null || email.trim().length() == 0)
            return "Email cannot be empty";

        String emailRegex =
            "^(?!.*\\.\\.)[a-zA-Z0-9][a-zA-Z0-9._%+-]*@gmail\\.com$";

        if (!email.matches(emailRegex))
            return "Invalid email. Only valid @gmail.com allowed";

        return "SUCCESS";
    }

    // ---------- PASSWORD VALIDATION ----------
    public String validatePassword(String password) {
        if (password == null) return "Password cannot be null";

        // Change {4,} to {4,8} to enforce the max limit
        String passwordRegex = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[@!#$%&*]).{4,8}$";

        if (!password.matches(passwordRegex))
            return "Password must have 1 Upper, 1 Lower, 1 Special & 4-8 chars";

        return "SUCCESS";
    }
}
