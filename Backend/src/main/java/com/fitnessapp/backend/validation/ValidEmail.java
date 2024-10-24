package com.fitnessapp.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EmailValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmail {
    String message() default "Bitte geben Sie eine gültige E-Mail-Adresse ein";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
