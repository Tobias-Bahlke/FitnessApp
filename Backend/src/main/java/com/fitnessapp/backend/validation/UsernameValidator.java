package com.fitnessapp.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class UsernameValidator implements ConstraintValidator<ValidUsername, String> {

    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9]{3,30}$";

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        return Pattern.compile(USERNAME_PATTERN).matcher(username).matches();
    }
}
