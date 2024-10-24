package com.fitnessapp.backend.dto;

import com.fitnessapp.backend.validation.ValidName;

public record RoleDTO(@ValidName String name) {
}
