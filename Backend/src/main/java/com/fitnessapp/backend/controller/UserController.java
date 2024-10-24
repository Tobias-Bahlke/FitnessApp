package com.fitnessapp.backend.controller;

import com.fitnessapp.backend.dto.RoleDTO;
import com.fitnessapp.backend.dto.UpdateUserDTO;
import com.fitnessapp.backend.exception.user.UserNotFoundException;
import com.fitnessapp.backend.model.User;
import com.fitnessapp.backend.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "user", description = "Endpunkte für die Benutzerverwaltung")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException("Benutzer nicht gefunden."));
        return ResponseEntity.ok(user);
    }

    @GetMapping("/by-email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        User user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/username/{email}")
    public ResponseEntity<String> getUsernameByEmail(@PathVariable String email) {
        String username = userService.getUsernameByEmail(email);
        return ResponseEntity.ok(username);
    }

    @GetMapping("/email/{username}")
    public ResponseEntity<String> getEmailByUsername(@PathVariable String username) {
        String email = userService.getEmailByUsername(username);
        return ResponseEntity.ok(email);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<String> updateUser(@PathVariable Long userId, @Valid @RequestBody UpdateUserDTO updateUserDTO) {
        userService.updateUser(userId, updateUserDTO);
        return ResponseEntity.ok("Benutzer erfolgreich aktualisiert.");
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok("Benutzer erfolgreich gelöscht.");
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<String> deleteUserAccount(Principal principal) {
        String username = principal.getName();
        User user = userService.getUserByUsername(username);
        userService.deleteUser(user.getId());
        return ResponseEntity.ok("Ihr Konto wurde erfolgreich gelöscht.");
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateUserRole(@PathVariable Long userId, @Valid @RequestBody RoleDTO roleDTO) {
        userService.updateUserRole(userId, roleDTO.name());
        return ResponseEntity.ok("Benutzerrolle erfolgreich aktualisiert.");
    }

    @PostMapping("/{userId}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> enableUser(@PathVariable Long userId) {
        userService.enableUser(userId);
        return ResponseEntity.ok("Benutzer erfolgreich aktiviert.");
    }

    @PostMapping("/{userId}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> disableUser(@PathVariable Long userId) {
        userService.disableUser(userId);
        return ResponseEntity.ok("Benutzer erfolgreich deaktiviert.");
    }

    @PostMapping("/{userId}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> banUser(@PathVariable Long userId, @RequestParam int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Die Anzahl der Tage muss positiv sein.");
        }
        LocalDateTime bannedUntil = LocalDateTime.now().plusDays(days);
        userService.banUser(userId, bannedUntil);
        return ResponseEntity.ok("Benutzer erfolgreich bis " + bannedUntil + " gesperrt.");
    }

    @PostMapping("/{userId}/unban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> unbanUser(@PathVariable Long userId) {
        userService.unbanUser(userId);
        return ResponseEntity.ok("Benutzer erfolgreich entsperrt.");
    }
}
