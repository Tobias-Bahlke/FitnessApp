package com.fitnessapp.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class EmailValidator implements ConstraintValidator<ValidEmail, String> {

    private static final String EMAIL_PATTERN = "^[\\w-.]+@[\\w-.]+\\.[a-z]{2,}$";

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return Pattern.compile(EMAIL_PATTERN).matcher(email).matches();
    }
}
