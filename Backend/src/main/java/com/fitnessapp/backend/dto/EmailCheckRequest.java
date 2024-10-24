package com.fitnessapp.backend.dto;

import com.fitnessapp.backend.validation.ValidEmail;

public record EmailCheckRequest(@ValidEmail String email) {
}
