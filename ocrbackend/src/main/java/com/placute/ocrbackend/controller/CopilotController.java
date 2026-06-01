package com.placute.ocrbackend.controller;

import com.placute.ocrbackend.dto.CopilotChatRequest;
import com.placute.ocrbackend.dto.CopilotChatResponse;
import com.placute.ocrbackend.service.CopilotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/copilot")
public class CopilotController {

    @Autowired
    private CopilotService copilotService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/chat")
    public ResponseEntity<CopilotChatResponse> chat(
            @RequestBody CopilotChatRequest request,
            Authentication authentication
    ) {
        String message = request != null ? request.getMessage() : null;
        return ResponseEntity.ok(copilotService.chat(message, authentication));
    }
}
