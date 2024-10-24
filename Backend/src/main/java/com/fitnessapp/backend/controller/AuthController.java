package com.fitnessapp.backend.controller;

import com.fitnessapp.backend.dto.*;
import com.fitnessapp.backend.model.Role;
import com.fitnessapp.backend.model.User;
import com.fitnessapp.backend.service.UserService;
import com.fitnessapp.backend.util.JwtUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "auth", description = "Endpunkte für die Authentifizierung")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final Map<String, Integer> failedLoginAttempts = new ConcurrentHashMap<>();

    @Value("${auth.max-login-attempts}")
    private int maxLoginAttempts;

    @PostMapping("/check-email")
    public ResponseEntity<AvailabilityResponse> checkEmailAvailability(@Valid @RequestBody EmailCheckRequest request) {
        boolean available = userService.isEmailAvailable(request.email());
        return ResponseEntity.ok(new AvailabilityResponse(available));
    }

    @PostMapping("/check-username")
    public ResponseEntity<AvailabilityResponse> checkUsernameAvailability(@Valid @RequestBody UsernameCheckRequest request) {
        boolean available = userService.isUsernameAvailable(request.username());
        return ResponseEntity.ok(new AvailabilityResponse(available));
    }

    @PostMapping("/signup")
    public ResponseEntity<String> registerUser(@Valid @RequestBody UserRegistrationDTO userRegistrationDTO) {
        userService.registerUser(userRegistrationDTO);
        return ResponseEntity.ok("Benutzer erfolgreich registriert! Bitte bestätige deine E-Mail-Adresse.");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> loginUser(@Valid @RequestBody UserLoginDTO userLoginDTO) {
        String username = userLoginDTO.username();
        User user = userService.getUserByUsername(username);

        if (user.getLockedUntil() != null && user.getLockedUntil().isBefore(LocalDateTime.now())) {
            failedLoginAttempts.remove(username);
            user.setLockedUntil(null);
            userService.saveUser(user);
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new LockedException("Konto ist bis " + user.getLockedUntil() + " gesperrt.");
        }

        if (failedLoginAttempts.containsKey(username) && failedLoginAttempts.get(username) >= maxLoginAttempts) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(1));
            userService.saveUser(user);
            throw new LockedException("Zu viele fehlgeschlagene Login-Versuche. Konto ist bis " + user.getLockedUntil() + " gesperrt.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, userLoginDTO.password())
            );
        } catch (BadCredentialsException e) {
            incrementFailedLoginAttempts(username);
            throw new BadCredentialsException("Falscher Benutzername oder Passwort", e);
        }

        resetFailedLoginAttempts(username);

        String jwt = jwtUtil.generateToken(user.getUsername(), getUserRole(user));
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        return ResponseEntity.ok(new AuthenticationResponse(jwt, refreshToken));
    }

    @PostMapping("/confirm-email")
    public ResponseEntity<Map<String, Object>> confirmEmail(@RequestBody Map<String, String> request, HttpServletResponse response) {
        String token = request.get("token");
        if (isTokenInvalid(token)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ungültiger Token"));
        }

        String username = jwtUtil.extractUsername(token);
        if (username == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ungültiger Token"));
        }

        User user = userService.getUserByUsername(username);

        if (user.getEnabled()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Benutzerkonto ist bereits aktiviert!"));
        }

        userService.enableUser(user.getId());

        String accessToken = jwtUtil.generateToken(username, getUserRole(user));
        String refreshToken = jwtUtil.generateRefreshToken(username);

        addRefreshTokenCookie(response, refreshToken);

        Map<String, Object> responseBody = Map.of(
                "message", "Benutzerkonto erfolgreich aktiviert!",
                "token", accessToken
        );

        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<String> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        userService.sendResetEmail(request.email());
        return ResponseEntity.ok("Ein Link zum Zurücksetzen des Passworts wurde an Ihre E-Mail-Adresse gesendet.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody PasswordResetTokenRequest request) {
        userService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok("Ihr Passwort wurde erfolgreich zurückgesetzt.");
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody PasswordChangeRequest request, Principal principal) {
        userService.changePassword(principal.getName(), request.currentPassword(), request.newPassword());
        return ResponseEntity.ok("Passwort erfolgreich geändert.");
    }

    @PostMapping("/reset-lock/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> resetAccountLock(@PathVariable String username) {
        User user = userService.getUserByUsername(username);

        userService.resetAccountLock(user.getId());

        failedLoginAttempts.remove(username);

        return ResponseEntity.ok("Benutzersperre und fehlgeschlagene Login-Versuche erfolgreich zurückgesetzt.");
    }

    private void incrementFailedLoginAttempts(String username) {
        failedLoginAttempts.merge(username, 1, Integer::sum);
    }

    private void resetFailedLoginAttempts(String username) {
        failedLoginAttempts.remove(username);
    }

    private boolean isTokenInvalid(String token) {
        return token == null || token.isEmpty();
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 3600);
        response.addCookie(refreshTokenCookie);
    }

    private String getUserRole(User user) {
        return user.getRoles().stream()
                .findFirst()
                .map(Role::getName)
                .orElse("USER");
    }
}
