package com.fitnessapp.backend.dto;

import com.fitnessapp.backend.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetTokenRequest(@NotBlank(message = "Das Token ist erforderlich") String token,
                                        @ValidPassword String newPassword) {
}
