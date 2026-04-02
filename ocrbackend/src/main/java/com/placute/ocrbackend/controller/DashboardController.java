package com.placute.ocrbackend.controller;

import com.placute.ocrbackend.dto.DashboardStatsDto;
import com.placute.ocrbackend.model.AppUser;
import com.placute.ocrbackend.repository.AppUserRepository;
import com.placute.ocrbackend.security.JwtService;
import com.placute.ocrbackend.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private DashboardService dashboardService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<?> getDashboard(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);
        AppUser user = appUserRepository.findTopByUsernameIgnoreCaseOrderByIdDesc(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("username", user.getUsername());
        response.put("role", user.getRole());

        switch (user.getRole()) {
            case POLICE -> response.put("message",
                    "Access all vehicle and user data.");
            case INSURANCE -> response.put("message",
                    "View insurance for all vehicles.");
            case PARKING -> response.put("message",
                    "Track parking entries and exits.");
        }

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }
}
