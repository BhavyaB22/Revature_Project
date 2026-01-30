package com.revconnect.test;

import static org.junit.Assert.*;
import org.junit.Test;

import com.revconnect.service.AuthService;

public class AuthServiceTest {

    private AuthService authService = new AuthService();

    /* ================= EMAIL TEST CASES ================= */

    @Test
    public void testValidEmail() {
        String result = authService.validateEmail("test@gmail.com");
        assertEquals("SUCCESS", result);
    }

    @Test
    public void testInvalidEmailWithoutGmail() {
        String result = authService.validateEmail("test@yahoo.com");
        assertEquals("Invalid email. Only valid @gmail.com allowed", result);
    }

    @Test
    public void testInvalidEmailWithDoubleDot() {
        String result = authService.validateEmail("te..st@gmail.com");
        assertEquals("Invalid email. Only valid @gmail.com allowed", result);
    }

    @Test
    public void testEmptyEmail() {
        String result = authService.validateEmail("");
        assertEquals("Email cannot be empty", result);
    }

    /* ================= PASSWORD TEST CASES ================= */

    @Test
    public void testValidPassword() {
        String result = authService.validatePassword("Ab@1");
        assertEquals("SUCCESS", result);
    }

    @Test
    public void testPasswordWithoutUppercase() {
        String result = authService.validatePassword("ab@1");
        assertEquals(
            "Password must have 1 Upper, 1 Lower, 1 Special & 4-8 chars",
            result
        );
    }

    @Test
    public void testPasswordWithoutSpecialChar() {
        String result = authService.validatePassword("Abc1");
        assertEquals(
            "Password must have 1 Upper, 1 Lower, 1 Special & 4-8 chars",
            result
        );
    }

    @Test
    public void testNullPassword() {
        String result = authService.validatePassword(null);
        assertEquals("Password cannot be null", result);
    }
}
