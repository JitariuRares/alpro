package com.placute.ocrbackend.dto;

import java.time.LocalDateTime;
import com.placute.ocrbackend.model.DetectionReviewStatus;

public class OcrHistoryDto {

    private Long id;
    private String plateNumber;
    private String brand;
    private String model;
    private String owner;
    private String imagePath;
    private LocalDateTime processedAt;
    private Double confidence;
    private BboxDto bbox;
    private DetectionReviewStatus reviewStatus;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewReason;

    public OcrHistoryDto(
            Long id,
            String plateNumber,
            String brand,
            String model,
            String owner,
            String imagePath,
            LocalDateTime processedAt,
            Double confidence,
            BboxDto bbox,
            DetectionReviewStatus reviewStatus,
            String reviewedBy,
            LocalDateTime reviewedAt,
            String reviewReason
    ) {
        this.id = id;
        this.plateNumber = plateNumber;
        this.brand = brand;
        this.model = model;
        this.owner = owner;
        this.imagePath = imagePath;
        this.processedAt = processedAt;
        this.confidence = confidence;
        this.bbox = bbox;
        this.reviewStatus = reviewStatus;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.reviewReason = reviewReason;
    }

    public Long getId() {
        return id;
    }
    public String getPlateNumber() {
        return plateNumber;
    }
    public String getBrand() {
        return brand;
    }
    public String getModel() {
        return model;
    }
    public String getOwner() {
        return owner;
    }
    public String getImagePath() {
        return imagePath;
    }
    public LocalDateTime getProcessedAt() {
        return processedAt;
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
