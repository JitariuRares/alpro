package com.placute.ocrbackend.repository;

import com.placute.ocrbackend.model.ParkingHistory;
import com.placute.ocrbackend.model.ParkingSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingHistoryRepository extends JpaRepository<ParkingHistory, Long> {
    List<ParkingHistory> findByLicensePlate_PlateNumber(String plateNumber);
    List<ParkingHistory> findByLicensePlate_PlateNumberOrderByEntryTimeDesc(String plateNumber);
    List<ParkingHistory> findTop20ByLicensePlate_PlateNumberOrderByEntryTimeDesc(String plateNumber);
    Optional<ParkingHistory> findTopByLicensePlate_PlateNumberAndStatusOrderByEntryTimeDesc(
            String plateNumber,
            ParkingSessionStatus status
    );
}
