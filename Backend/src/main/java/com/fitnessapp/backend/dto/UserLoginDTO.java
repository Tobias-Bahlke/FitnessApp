package com.fitnessapp.backend.dto;

import com.fitnessapp.backend.validation.ValidPassword;
import com.fitnessapp.backend.validation.ValidUsername;

public record UserLoginDTO(@ValidUsername String username, @ValidPassword String password) {
}
