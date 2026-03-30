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

    public OcrPlateDto(Long id, String plateNumber, String brand, String model, String owner,
                       String imagePath, LocalDateTime detectedAt, String user, String role) {
        this.id = id;
        this.plateNumber = plateNumber;
        this.brand = brand;
        this.model = model;
        this.owner = owner;
        this.imagePath = imagePath;
        this.detectedAt = detectedAt;
        this.user = user;
        this.role = role;
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
}
