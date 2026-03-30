package com.placute.ocrbackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "ocr_history")
public class OcrHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "license_plate_id", nullable = false)
    private LicensePlate licensePlate;

    @Column(name = "filename")
    private String filename;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public OcrHistory() { }


    public OcrHistory(LicensePlate licensePlate, String filename, LocalDateTime processedAt) {
        this.licensePlate = licensePlate;
        this.filename = filename;
        this.processedAt = processedAt;
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

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
