package com.fitnessapp.backend.dto;

public record AuthenticationResponse(String jwt, String refreshToken) {
}
