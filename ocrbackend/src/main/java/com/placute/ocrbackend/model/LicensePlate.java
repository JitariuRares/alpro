package com.placute.ocrbackend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "license_plate")
public class LicensePlate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plate_number", nullable = false, unique = true)
    private String plateNumber;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "brand")
    private String brand;

    @Column(name = "model")
    private String model;

    @Column(name = "owner")
    private String owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private AppUser user;

    @OneToMany(mappedBy = "licensePlate", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<ParkingHistory> parkingHistory;

    @OneToMany(mappedBy = "licensePlate", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Insurance> insurances;

    public LicensePlate() { }

    public LicensePlate(String plateNumber, String imagePath) {
        this.plateNumber = plateNumber;
        this.imagePath = imagePath;
        this.detectedAt = LocalDateTime.now();
    }


    public Long getId() {
        return id;
    }

    public String getPlateNumber() {
        return plateNumber;
    }
    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }
    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }

    public String getImagePath() {
        return imagePath;
    }
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getBrand() {
        return brand;
    }
    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }
    public void setModel(String model) {
        this.model = model;
    }

    public String getOwner() {
        return owner;
    }
    public void setOwner(String owner) {
        this.owner = owner;
    }

    public AppUser getUser() {
        return user;
    }
    public void setUser(AppUser user) {
        this.user = user;
    }

    public List<ParkingHistory> getParkingHistory() {
        return parkingHistory;
    }
    public void setParkingHistory(List<ParkingHistory> parkingHistory) {
        this.parkingHistory = parkingHistory;
    }

    public List<Insurance> getInsurances() {
        return insurances;
    }
    public void setInsurances(List<Insurance> insurances) {
        this.insurances = insurances;
    }
}
