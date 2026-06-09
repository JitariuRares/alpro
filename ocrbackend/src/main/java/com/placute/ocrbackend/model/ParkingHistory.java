package com.placute.ocrbackend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ParkingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime entryTime;
    private LocalDateTime exitTime;

    @Column(name = "entry_image_path")
    private String entryImagePath;

    @Column(name = "exit_image_path")
    private String exitImagePath;

    @Column(name = "parking_zone", length = 50)
    private String parkingZone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ParkingSessionStatus status;

    @ManyToOne
    @JoinColumn(name = "plate_id")
    @JsonBackReference
    private LicensePlate licensePlate;


    public Long getId() {
        return id;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public void setExitTime(LocalDateTime exitTime) {
        this.exitTime = exitTime;
    }

    public String getEntryImagePath() {
        return entryImagePath;
    }

    public void setEntryImagePath(String entryImagePath) {
        this.entryImagePath = entryImagePath;
    }

    public String getExitImagePath() {
        return exitImagePath;
    }

    public void setExitImagePath(String exitImagePath) {
        this.exitImagePath = exitImagePath;
    }

    public String getParkingZone() {
        return parkingZone;
    }

    public void setParkingZone(String parkingZone) {
        this.parkingZone = parkingZone;
    }

    public ParkingSessionStatus getStatus() {
        return status;
    }

    public void setStatus(ParkingSessionStatus status) {
        this.status = status;
    }

    public LicensePlate getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(LicensePlate licensePlate) {
        this.licensePlate = licensePlate;
    }
}
