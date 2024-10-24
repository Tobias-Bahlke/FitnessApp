package com.fitnessapp.backend.dto;

import com.fitnessapp.backend.validation.ValidPassword;

public record PasswordChangeRequest(@ValidPassword String currentPassword, @ValidPassword String newPassword) {
}
