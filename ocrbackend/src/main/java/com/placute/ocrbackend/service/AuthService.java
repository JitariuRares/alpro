package com.placute.ocrbackend.service;

import com.placute.ocrbackend.auth.AuthResponse;
import com.placute.ocrbackend.dto.LoginRequest;
import com.placute.ocrbackend.dto.RegisterRequest;
import com.placute.ocrbackend.model.AppUser;
import com.placute.ocrbackend.model.UserRole;
import com.placute.ocrbackend.repository.AppUserRepository;
import com.placute.ocrbackend.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Value("${auth.public-register.allowed-roles:PARKING}")
    private String allowedPublicRegisterRoles;

    public String register(RegisterRequest request) {
        if (request == null) {
            throw new RuntimeException("Cererea de inregistrare este invalida");
        }

        String username = request.getUsername() == null ? null : request.getUsername().trim();
        if (username == null || username.isBlank()) {
            throw new RuntimeException("Username invalid");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new RuntimeException("Parola este obligatorie");
        }
        if (request.getPassword().length() < 8) {
            throw new RuntimeException("Parola trebuie sa aiba minim 8 caractere");
        }

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new RuntimeException("Username-ul exista deja");
        }

        UserRole requestedRole = request.getRole() == null ? UserRole.PARKING : request.getRole();
        Set<UserRole> allowedRoles = parseAllowedRegisterRoles();
        if (!allowedRoles.contains(requestedRole)) {
            throw new RuntimeException("Rolul selectat nu este permis pentru inregistrare publica");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(requestedRole);

        userRepository.save(user);
        return "User inregistrat cu succes!";
    }

    public AuthResponse login(LoginRequest request) {
        if (request == null) {
            throw new RuntimeException("Cererea de autentificare este invalida");
        }

        String username = request.getUsername() == null ? null : request.getUsername().trim();
        if (username == null || username.isBlank()) {
            throw new RuntimeException("Username invalid");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new RuntimeException("Parola invalida");
        }

        AppUser user = userRepository.findTopByUsernameIgnoreCaseOrderByIdDesc(username)
                .orElse(null);
        if (user == null) {
            throw new RuntimeException("Userul nu exista");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Parola incorecta");
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token);
    }

    private Set<UserRole> parseAllowedRegisterRoles() {
        Set<UserRole> parsed = Arrays.stream(allowedPublicRegisterRoles.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(value -> {
                    try {
                        return UserRole.valueOf(value.toUpperCase());
                    } catch (IllegalArgumentException ignored) {
                        return null;
                    }
                })
                .filter(role -> role != null)
                .collect(Collectors.toSet());

        if (parsed.isEmpty()) {
            return Set.of(UserRole.PARKING);
        }
        return parsed;
    }
}
