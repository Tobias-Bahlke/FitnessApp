package com.fitnessapp.backend.dto;

import com.fitnessapp.backend.validation.ValidEmail;
import com.fitnessapp.backend.validation.ValidName;

public record UpdateUserDTO(@ValidName String firstname, @ValidName String lastname, @ValidEmail String email) {
}
