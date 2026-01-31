package com.revconnect.exceptions;
// Custom exception for validation errors (email, phone, password)
public class InvalidInputException extends Exception {
    public InvalidInputException(String message) {
        super(message);
    }
}