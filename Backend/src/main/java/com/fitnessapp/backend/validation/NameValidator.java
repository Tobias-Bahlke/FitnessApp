package com.fitnessapp.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class NameValidator implements ConstraintValidator<ValidName, String> {

    private static final String NAME_PATTERN = "^[a-zA-Z]{2,30}$";

    @Override
    public boolean isValid(String name, ConstraintValidatorContext context) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return Pattern.compile(NAME_PATTERN).matcher(name).matches();
    }
}
