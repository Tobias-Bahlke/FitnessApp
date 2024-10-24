package com.fitnessapp.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UsernameValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUsername {
    String message() default "Der Benutzername muss zwischen 3 und 30 Zeichen lang sein und darf nur Buchstaben und Zahlen enthalten.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
