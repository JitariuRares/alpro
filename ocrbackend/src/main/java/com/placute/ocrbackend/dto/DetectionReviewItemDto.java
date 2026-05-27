package com.placute.ocrbackend.dto;

import com.placute.ocrbackend.model.DetectionReviewStatus;

import java.time.LocalDateTime;

public record DetectionReviewItemDto(
        String sourceType,
        Long sourceId,
        String plateNumber,
        Double confidence,
        Long occurrenceCount,
        LocalDateTime detectedAt,
        String sourceLabel,
        String detail,
        DetectionReviewStatus reviewStatus,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String reviewReason
) {
}
