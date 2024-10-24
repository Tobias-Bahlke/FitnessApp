package com.fitnessapp.backend.exception.user;

public class UserNotBannedException extends RuntimeException {
    public UserNotBannedException(String message) {
        super(message);
    }
}
