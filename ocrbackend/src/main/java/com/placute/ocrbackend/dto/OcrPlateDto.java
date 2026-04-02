package com.placute.ocrbackend.dto;

import java.time.LocalDateTime;

public class OcrPlateDto {
    private Long id;
    private String plateNumber;
    private String brand;
    private String model;
    private String owner;
    private String imagePath;
    private LocalDateTime detectedAt;
    private String user;
    private String role;
    private Double confidence;
    private BboxDto bbox;

    public OcrPlateDto(Long id, String plateNumber, String brand, String model, String owner,
                       String imagePath, LocalDateTime detectedAt, String user, String role,
                       Double confidence, BboxDto bbox) {
        this.id = id;
        this.plateNumber = plateNumber;
        this.brand = brand;
        this.model = model;
        this.owner = owner;
        this.imagePath = imagePath;
        this.detectedAt = detectedAt;
        this.user = user;
        this.role = role;
        this.confidence = confidence;
        this.bbox = bbox;
    }

    public Long getId() { return id; }
    public String getPlateNumber() { return plateNumber; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public String getOwner() { return owner; }
    public String getImagePath() { return imagePath; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public String getUser() { return user; }
    public String getRole() { return role; }
    public Double getConfidence() { return confidence; }
    public BboxDto getBbox() { return bbox; }

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

        public Integer getX() { return x; }
        public Integer getY() { return y; }
        public Integer getW() { return w; }
        public Integer getH() { return h; }
    }
}
