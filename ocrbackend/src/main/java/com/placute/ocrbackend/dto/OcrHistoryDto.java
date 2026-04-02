package com.placute.ocrbackend.dto;

import java.time.LocalDateTime;

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

    public OcrHistoryDto(
            Long id,
            String plateNumber,
            String brand,
            String model,
            String owner,
            String imagePath,
            LocalDateTime processedAt,
            Double confidence,
            BboxDto bbox
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
