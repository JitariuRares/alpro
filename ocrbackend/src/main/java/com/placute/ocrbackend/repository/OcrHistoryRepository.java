package com.placute.ocrbackend.repository;

import com.placute.ocrbackend.model.OcrHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface OcrHistoryRepository extends JpaRepository<OcrHistory, Long> {

    @Query("select h from OcrHistory h join fetch h.licensePlate order by h.processedAt desc")
    List<OcrHistory> findAllWithLicensePlateOrderByProcessedAtDesc();

    @Query("""
            select h
            from OcrHistory h
            join fetch h.licensePlate lp
            where lower(lp.plateNumber) like lower(concat('%', :fragment, '%'))
            order by h.processedAt desc
            """)
    List<OcrHistory> findByPlateNumberContainingWithLicensePlateOrderByProcessedAtDesc(@Param("fragment") String fragment);

    Optional<OcrHistory> findTopByLicensePlate_IdOrderByProcessedAtDesc(Long licensePlateId);
}
