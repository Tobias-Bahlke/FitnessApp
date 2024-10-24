package com.fitnessapp.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NameValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidName {
    String message() default "Der Name darf nur Buchstaben enthalten und muss zwischen 2 und 30 Zeichen lang sein.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
