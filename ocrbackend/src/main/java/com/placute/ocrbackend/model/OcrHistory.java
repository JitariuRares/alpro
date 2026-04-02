package com.placute.ocrbackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(
        name = "ocr_history",
        uniqueConstraints = @UniqueConstraint(name = "uk_ocr_history_license_plate", columnNames = "license_plate_id")
)
public class OcrHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "license_plate_id", nullable = false)
    private LicensePlate licensePlate;

    @Column(name = "filename")
    private String filename;

    @Column(name = "image_hash", length = 64)
    private String imageHash;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "bbox_x")
    private Integer bboxX;

    @Column(name = "bbox_y")
    private Integer bboxY;

    @Column(name = "bbox_w")
    private Integer bboxW;

    @Column(name = "bbox_h")
    private Integer bboxH;

    public OcrHistory() { }


    public OcrHistory(
            LicensePlate licensePlate,
            String filename,
            String imageHash,
            LocalDateTime processedAt,
            Double confidence,
            Integer bboxX,
            Integer bboxY,
            Integer bboxW,
            Integer bboxH
    ) {
        this.licensePlate = licensePlate;
        this.filename = filename;
        this.imageHash = imageHash;
        this.processedAt = processedAt;
        this.confidence = confidence;
        this.bboxX = bboxX;
        this.bboxY = bboxY;
        this.bboxW = bboxW;
        this.bboxH = bboxH;
    }


    public Long getId() {
        return id;
    }

    public LicensePlate getLicensePlate() {
        return licensePlate;
    }
    public void setLicensePlate(LicensePlate licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getFilename() {
        return filename;
    }
    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getImageHash() {
        return imageHash;
    }
    public void setImageHash(String imageHash) {
        this.imageHash = imageHash;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public Double getConfidence() {
        return confidence;
    }
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Integer getBboxX() {
        return bboxX;
    }
    public void setBboxX(Integer bboxX) {
        this.bboxX = bboxX;
    }

    public Integer getBboxY() {
        return bboxY;
    }
    public void setBboxY(Integer bboxY) {
        this.bboxY = bboxY;
    }

    public Integer getBboxW() {
        return bboxW;
    }
    public void setBboxW(Integer bboxW) {
        this.bboxW = bboxW;
    }

    public Integer getBboxH() {
        return bboxH;
    }
    public void setBboxH(Integer bboxH) {
        this.bboxH = bboxH;
    }
}
