package com.fitnessapp.backend.dto;

import com.fitnessapp.backend.validation.ValidEmail;
import com.fitnessapp.backend.validation.ValidName;
import com.fitnessapp.backend.validation.ValidPassword;
import com.fitnessapp.backend.validation.ValidUsername;

public record UserRegistrationDTO(@ValidUsername String username, @ValidName String firstname,
                                  @ValidName String lastname, @ValidEmail String email,
                                  @ValidPassword String password) {

}
