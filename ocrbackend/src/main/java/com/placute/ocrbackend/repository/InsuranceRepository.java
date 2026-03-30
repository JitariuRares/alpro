package com.placute.ocrbackend.repository;

import com.placute.ocrbackend.model.Insurance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsuranceRepository extends JpaRepository<Insurance, Long> {
    List<Insurance> findByLicensePlate_PlateNumber(String plateNumber);
}
