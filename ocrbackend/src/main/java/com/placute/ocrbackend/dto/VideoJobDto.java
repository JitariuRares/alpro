package com.placute.ocrbackend.dto;

import com.placute.ocrbackend.model.VideoJobStatus;

import java.time.LocalDateTime;

public class VideoJobDto {

    private Long id;
    private String sourceFilename;
    private String storedFilename;
    private VideoJobStatus status;
    private Integer progressPercent;
    private String errorMessage;
    private Long detectionCount;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public VideoJobDto(
            Long id,
            String sourceFilename,
            String storedFilename,
            VideoJobStatus status,
            Integer progressPercent,
            String errorMessage,
            Long detectionCount,
            LocalDateTime createdAt,
            LocalDateTime startedAt,
            LocalDateTime completedAt
    ) {
        this.id = id;
        this.sourceFilename = sourceFilename;
        this.storedFilename = storedFilename;
        this.status = status;
        this.progressPercent = progressPercent;
        this.errorMessage = errorMessage;
        this.detectionCount = detectionCount;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public Long getId() {
        return id;
    }

    public String getSourceFilename() {
        return sourceFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public VideoJobStatus getStatus() {
        return status;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Long getDetectionCount() {
        return detectionCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
