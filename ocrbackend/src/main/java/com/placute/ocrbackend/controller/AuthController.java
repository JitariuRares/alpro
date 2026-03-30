package com.placute.ocrbackend.controller;

import com.placute.ocrbackend.auth.AuthRequest;
import com.placute.ocrbackend.auth.AuthResponse;
import com.placute.ocrbackend.dto.LoginRequest;
import com.placute.ocrbackend.dto.RegisterRequest;
import com.placute.ocrbackend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
