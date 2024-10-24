package com.fitnessapp.backend.dto;

import com.fitnessapp.backend.validation.ValidUsername;

public record UsernameCheckRequest(@ValidUsername String username) {
}
