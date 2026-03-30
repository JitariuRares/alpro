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

    public OcrHistoryDto(Long id, String plateNumber, String brand, String model, String owner, String imagePath, LocalDateTime processedAt) {
        this.id = id;
        this.plateNumber = plateNumber;
        this.brand = brand;
        this.model = model;
        this.owner = owner;
        this.imagePath = imagePath;
        this.processedAt = processedAt;
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
}
