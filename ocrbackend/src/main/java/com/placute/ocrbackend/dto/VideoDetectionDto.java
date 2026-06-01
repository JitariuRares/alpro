package com.placute.ocrbackend.dto;

import com.placute.ocrbackend.model.DetectionReviewStatus;

import java.time.LocalDateTime;

public class VideoDetectionDto {

    private Long id;
    private Integer frameIndex;
    private Long timestampMs;
    private Integer trackId;
    private String plateText;
    private Double confidence;
    private BboxDto bbox;
    private DetectionReviewStatus reviewStatus;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewReason;
    private VehicleAttributesDto aiAttributes;

    public VideoDetectionDto(
            Long id,
            Integer frameIndex,
            Long timestampMs,
            Integer trackId,
            String plateText,
            Double confidence,
            BboxDto bbox,
            DetectionReviewStatus reviewStatus,
            String reviewedBy,
            LocalDateTime reviewedAt,
            String reviewReason,
            VehicleAttributesDto aiAttributes
    ) {
        this.id = id;
        this.frameIndex = frameIndex;
        this.timestampMs = timestampMs;
        this.trackId = trackId;
        this.plateText = plateText;
        this.confidence = confidence;
        this.bbox = bbox;
        this.reviewStatus = reviewStatus;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.reviewReason = reviewReason;
        this.aiAttributes = aiAttributes;
    }

    public Long getId() {
        return id;
    }

    public Integer getFrameIndex() {
        return frameIndex;
    }

    public Long getTimestampMs() {
        return timestampMs;
    }

    public Integer getTrackId() {
        return trackId;
    }

    public String getPlateText() {
        return plateText;
    }

    public Double getConfidence() {
        return confidence;
    }

    public BboxDto getBbox() {
        return bbox;
    }

    public DetectionReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewReason() {
        return reviewReason;
    }

    public VehicleAttributesDto getAiAttributes() {
        return aiAttributes;
    }

    public static class BboxDto {
        private Integer x;
        private Integer y;
        private Integer w;
        private Integer h;

        public BboxDto(Integer x, Integer y, Integer w, Integer h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public Integer getX() {
            return x;
        }

        public Integer getY() {
            return y;
        }

        public Integer getW() {
            return w;
        }

        public Integer getH() {
            return h;
        }
    }
}
