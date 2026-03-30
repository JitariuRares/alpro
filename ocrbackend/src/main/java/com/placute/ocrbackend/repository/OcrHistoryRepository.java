package com.placute.ocrbackend.repository;

import com.placute.ocrbackend.model.OcrHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface OcrHistoryRepository extends JpaRepository<OcrHistory, Long> {

    List<OcrHistory> findAllByOrderByProcessedAtDesc();

    List<OcrHistory> findByLicensePlate_PlateNumberContainingIgnoreCaseOrderByProcessedAtDesc(String fragment);
}
