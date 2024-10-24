package com.fitnessapp.backend.configuration.initializer;

import com.fitnessapp.backend.model.Role;
import com.fitnessapp.backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        List<String> roles = Arrays.asList("USER", "ADMIN");

        roles.forEach(roleName -> {
            if (!roleRepository.existsByName(roleName)) {
                Role role = Role.builder().name(roleName).build();
                roleRepository.save(role);
            }
        });
    }
}
