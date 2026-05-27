package com.placute.ocrbackend.repository;

import com.placute.ocrbackend.model.Insurance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InsuranceRepository extends JpaRepository<Insurance, Long> {
    List<Insurance> findByLicensePlate_PlateNumber(String plateNumber);
    List<Insurance> findByLicensePlate_PlateNumberOrderByValidToDesc(String plateNumber);
    long countByValidFromLessThanEqualAndValidToGreaterThanEqual(LocalDate validFrom, LocalDate validTo);
    long countByValidToBefore(LocalDate date);
    long countByValidToBetween(LocalDate startDate, LocalDate endDate);
}
