package com.fitnessapp.backend.dto;

import com.fitnessapp.backend.validation.ValidEmail;

public record PasswordResetRequest(@ValidEmail String email) {
}
