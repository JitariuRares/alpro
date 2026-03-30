package com.placute.ocrbackend.dto;

import com.placute.ocrbackend.model.UserRole;
import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private UserRole role;
}
