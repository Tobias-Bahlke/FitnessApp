package com.fitnessapp.backend.service;

import com.fitnessapp.backend.dto.UpdateUserDTO;
import com.fitnessapp.backend.dto.UserRegistrationDTO;
import com.fitnessapp.backend.exception.authentication.*;
import com.fitnessapp.backend.exception.user.UserAlreadyEnabledException;
import com.fitnessapp.backend.exception.user.UserNotBannedException;
import com.fitnessapp.backend.exception.user.UserNotFoundException;
import com.fitnessapp.backend.model.Role;
import com.fitnessapp.backend.model.User;
import com.fitnessapp.backend.repository.RoleRepository;
import com.fitnessapp.backend.repository.UserRepository;
import com.fitnessapp.backend.util.JwtUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final JwtUtil jwtUtil;

    @Value("${app.client.url}")
    private String frontendUrl;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Benutzer mit E-Mail " + email + " wurde nicht gefunden."));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Benutzer mit Username " + username + " wurde nicht gefunden."));
    }

    public String getUsernameByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(User::getUsername)
                .orElseThrow(() -> new UserNotFoundException("Benutzer für E-Mail-Adresse " + email + " wurde nicht gefunden."));
    }

    public String getEmailByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(User::getEmail)
                .orElseThrow(() -> new UserNotFoundException("E-Mail-Adresse für Benutzer " + username + " wurde nicht gefunden."));
    }

    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = getUserByUsername(username);

        if (!user.getEnabled()) {
            throw new DisabledException("Der Benutzer ist nicht aktiviert.");
        }

        if (user.getBanned()) {
            handleBannedUser(user);
        }

        Set<GrantedAuthority> authorities = user.getRoles()
                .stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toSet());

        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), authorities);
    }

    @Transactional
    public void registerUser(UserRegistrationDTO userRegistrationDTO) {
        if (!isUsernameAvailable(userRegistrationDTO.username())) {
            throw new UsernameAlreadyExistsException("Der Benutzername ist bereits vergeben!");
        }

        if (!isEmailAvailable(userRegistrationDTO.email())) {
            throw new EmailAlreadyExistsException("Die Email wird bereits verwendet!");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RoleNotFoundException("Die Rolle USER wurde nicht gefunden."));

        User user = User.builder()
                .email(userRegistrationDTO.email())
                .username(userRegistrationDTO.username())
                .firstname(userRegistrationDTO.firstname())
                .lastname(userRegistrationDTO.lastname())
                .password(passwordEncoder.encode(userRegistrationDTO.password()))
                .enabled(false)
                .banned(false)
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .build();

        User savedUser = userRepository.save(user);

        sendConfirmationEmail(savedUser);
    }

    @Transactional
    public void saveUser(User user) {
        userRepository.save(user);
    }

    @Transactional
    public void updateUser(Long userId, UpdateUserDTO updateUserDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Benutzer mit ID " + userId + " wurde nicht gefunden."));

        if (!user.getEmail().equals(updateUserDTO.email()) && !isEmailAvailable(updateUserDTO.email())) {
            throw new EmailAlreadyExistsException("Die Email wird bereits verwendet!");
        }

        user.setFirstname(updateUserDTO.firstname());
        user.setLastname(updateUserDTO.lastname());
        user.setEmail(updateUserDTO.email());
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Benutzer mit ID " + userId + " wurde nicht gefunden."));
        userRepository.delete(user);
    }

    @Transactional
    public void banUser(Long userId, LocalDateTime bannedUntil) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Benutzer mit ID " + userId + " wurde nicht gefunden."));
        user.setBanned(true);
        user.setBannedUntil(bannedUntil);
        userRepository.save(user);
    }

    @Transactional
    public void unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Benutzer mit ID " + userId + " wurde nicht gefunden."));
        if (!user.getBanned()) {
            throw new UserNotBannedException("Der Benutzer ist nicht gesperrt.");
        }
        user.setBanned(false);
        user.setBannedUntil(null);
        userRepository.save(user);
    }

    @Transactional
    public void enableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Benutzer mit ID " + userId + " wurde nicht gefunden."));
        if (user.getEnabled()) {
            throw new UserAlreadyEnabledException("Benutzerkonto ist bereits aktiviert.");
        }
        user.setEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void disableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Benutzer mit ID " + userId + " wurde nicht gefunden."));
        if (!user.getEnabled()) {
            throw new RuntimeException("Benutzerkonto ist bereits deaktiviert.");
        }
        user.setEnabled(false);
        userRepository.save(user);
    }

    @Transactional
    public void resetAccountLock(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Benutzer mit ID " + userId + " wurde nicht gefunden."));
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    @Transactional
    public void updateUserRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Benutzer mit ID " + userId + " wurde nicht gefunden."));

        Role newRole = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RoleNotFoundException("Die Rolle " + roleName + " wurde nicht gefunden."));

        user.getRoles().clear();
        user.getRoles().add(newRole);
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        String username = jwtUtil.extractUsername(token);
        User user = getUserByUsername(username);

        if (!jwtUtil.validateToken(token, loadUserByUsername(username))) {
            throw new InvalidTokenException("Das Token ist ungültig oder abgelaufen.");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new PasswordMismatchException("Das neue Passwort darf nicht mit dem alten Passwort identisch sein.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = getUserByUsername(username);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new PasswordMismatchException("Das aktuelle Passwort ist falsch.");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new PasswordMismatchException("Das neue Passwort darf nicht mit dem alten Passwort identisch sein.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        sendPasswordChangeNotification(user);
    }

    @Transactional
    public void sendConfirmationEmail(User user) {
        String token = jwtUtil.generateToken(user.getUsername(), getUserRole(user));
        String confirmUrl = frontendUrl + "/confirm-email?token=" + token;
        String subject = "Bestätigung Ihrer E-Mail-Adresse";
        String htmlContent = """
                <html>
                <body>
                    <h1>Bestätigung Ihrer E-Mail-Adresse</h1>
                    <p>Klicken Sie bitte auf den folgenden Link, um Ihre E-Mail-Adresse zu bestätigen:</p>
                    <a href="%s">E-Mail bestätigen</a>
                    <br><br>
                </body>
                </html>
                """.formatted(confirmUrl);
        try {
            sendHtmlMessage(user.getEmail(), subject, htmlContent);
        } catch (MessagingException e) {
            throw new RuntimeException("Fehler beim Senden der Bestätigungs-E-Mail.", e);
        }
    }

    @Transactional
    public void sendResetEmail(String email) {
        User user = getUserByEmail(email);
        String token = jwtUtil.generateToken(user.getUsername(), getUserRole(user));
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        String subject = "Passwort zurücksetzen";
        String htmlContent = """
                <html>
                <body>
                    <h1>Passwort zurücksetzen</h1>
                    <p>Klicken Sie bitte auf den folgenden Link, um Ihr Passwort zurückzusetzen:</p>
                    <a href="%s">Passwort zurücksetzen</a>
                    <br><br>
                </body>
                </html>
                """.formatted(resetUrl);
        try {
            sendHtmlMessage(user.getEmail(), subject, htmlContent);
        } catch (MessagingException e) {
            throw new RuntimeException("Fehler beim Senden der Passwort-Zurücksetzen-E-Mail.", e);
        }
    }

    private String getUserRole(User user) {
        return user.getRoles().stream()
                .findFirst()
                .map(Role::getName)
                .orElse("USER");
    }

    private void handleBannedUser(User user) {
        if (user.getBannedUntil() != null) {
            if (user.getBannedUntil().isAfter(LocalDateTime.now())) {
                throw new LockedException("Der Benutzer ist bis " + user.getBannedUntil() + " gesperrt.");
            } else {
                user.setBanned(false);
                user.setBannedUntil(null);
                userRepository.save(user);
            }
        } else {
            throw new LockedException("Der Benutzer ist dauerhaft gesperrt.");
        }
    }

    private void sendHtmlMessage(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    private void sendPasswordChangeNotification(User user) {
        String subject = "Passwort erfolgreich geändert";
        String htmlContent = """
                <html>
                <body>
                    <h1>Ihr Passwort wurde erfolgreich geändert</h1>
                    <p>Falls Sie dies nicht veranlasst haben, wenden Sie sich bitte sofort an den Support.</p>
                </body>
                </html>
                """;
        try {
            sendHtmlMessage(user.getEmail(), subject, htmlContent);
        } catch (MessagingException e) {
            throw new RuntimeException("Fehler beim Senden der Benachrichtigung über die Passwortänderung.", e);
        }
    }
}
