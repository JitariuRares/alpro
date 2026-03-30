package com.placute.ocrbackend.repository;

import com.placute.ocrbackend.model.ParkingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingHistoryRepository extends JpaRepository<ParkingHistory, Long> {
    List<ParkingHistory> findByLicensePlate_PlateNumber(String plateNumber);
}
