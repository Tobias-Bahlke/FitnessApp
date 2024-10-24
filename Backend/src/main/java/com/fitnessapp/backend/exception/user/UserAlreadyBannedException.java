package com.fitnessapp.backend.exception.user;

public class UserAlreadyBannedException extends RuntimeException {
    public UserAlreadyBannedException(String message) {
        super(message);
    }
}
