package com.fitnessapp.backend.repository;

import com.fitnessapp.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Boolean existsByEmail(String email);
    Boolean existsByUsername(String username);

    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
}
