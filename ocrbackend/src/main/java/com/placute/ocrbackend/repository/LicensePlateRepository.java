package com.placute.ocrbackend.repository;

import com.placute.ocrbackend.model.LicensePlate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface LicensePlateRepository extends JpaRepository<LicensePlate, Long> {
    List<LicensePlate> findByPlateNumber(String plateNumber);
    List<LicensePlate> findByDetectedAtAfter(LocalDateTime date);
}
