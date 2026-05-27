package com.placute.ocrbackend.dto;

import com.placute.ocrbackend.model.DetectionReviewStatus;

public record DetectionReviewUpdateRequest(DetectionReviewStatus status, String plateNumber) {
}
