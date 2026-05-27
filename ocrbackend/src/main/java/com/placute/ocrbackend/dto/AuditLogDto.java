package com.placute.ocrbackend.dto;

import java.time.LocalDateTime;

public record AuditLogDto(
        Long id,
        String actorUsername,
        String action,
        String targetPlateNumber,
        String details,
        LocalDateTime createdAt
) {
}
