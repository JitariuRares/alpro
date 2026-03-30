package com.placute.ocrbackend.service;

import com.placute.ocrbackend.auth.AuthResponse;
import com.placute.ocrbackend.dto.LoginRequest;
import com.placute.ocrbackend.dto.RegisterRequest;
import com.placute.ocrbackend.model.AppUser;
import com.placute.ocrbackend.repository.AppUserRepository;
import com.placute.ocrbackend.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    public String register(RegisterRequest request) {
        String username = request.getUsername() == null ? null : request.getUsername().trim();
        if (username == null || username.isBlank()) {
            throw new RuntimeException("Username invalid");
        }

        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username-ul exista deja");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        userRepository.save(user);
        return "User inregistrat cu succes!";
    }

    public AuthResponse login(LoginRequest request) {
        List<AppUser> users = userRepository.findAllByUsernameOrderByIdDesc(request.getUsername());
        if (users.isEmpty()) {
            throw new RuntimeException("Userul nu exista");
        }

        AppUser matched = users.stream()
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Parola incorecta"));

        String token = jwtService.generateToken(matched);
        return new AuthResponse(token);
    }
}
