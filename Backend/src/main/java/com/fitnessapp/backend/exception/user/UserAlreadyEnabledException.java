package com.fitnessapp.backend.exception.user;

public class UserAlreadyEnabledException extends RuntimeException {
    public UserAlreadyEnabledException(String message) {
        super(message);
    }
}
