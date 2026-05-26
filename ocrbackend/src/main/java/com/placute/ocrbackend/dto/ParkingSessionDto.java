package com.placute.ocrbackend.dto;

import com.placute.ocrbackend.model.ParkingSessionStatus;

import java.time.LocalDateTime;

public class ParkingSessionDto {

    private Long id;
    private String plateNumber;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private ParkingSessionStatus status;
    private Long durationMinutes;
    private boolean hasEntryImage;
    private boolean hasExitImage;

    public ParkingSessionDto(
            Long id,
            String plateNumber,
            LocalDateTime entryTime,
            LocalDateTime exitTime,
            ParkingSessionStatus status,
            Long durationMinutes,
            boolean hasEntryImage,
            boolean hasExitImage
    ) {
        this.id = id;
        this.plateNumber = plateNumber;
        this.entryTime = entryTime;
        this.exitTime = exitTime;
        this.status = status;
        this.durationMinutes = durationMinutes;
        this.hasEntryImage = hasEntryImage;
        this.hasExitImage = hasExitImage;
    }

    public Long getId() {
        return id;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public ParkingSessionStatus getStatus() {
        return status;
    }

    public Long getDurationMinutes() {
        return durationMinutes;
    }

    public boolean isHasEntryImage() {
        return hasEntryImage;
    }

    public boolean isHasExitImage() {
        return hasExitImage;
    }
}
