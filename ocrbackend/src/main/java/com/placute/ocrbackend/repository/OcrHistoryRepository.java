package com.placute.ocrbackend.repository;

import com.placute.ocrbackend.model.OcrHistory;
import com.placute.ocrbackend.model.DetectionReviewStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface OcrHistoryRepository extends JpaRepository<OcrHistory, Long> {

    @Query("select h from OcrHistory h join fetch h.licensePlate order by h.processedAt desc")
    List<OcrHistory> findAllWithLicensePlateOrderByProcessedAtDesc();

    @Query("select h from OcrHistory h join fetch h.licensePlate where h.id = :id")
    Optional<OcrHistory> findByIdWithLicensePlate(@Param("id") Long id);

    @Query("""
            select h
            from OcrHistory h
            join fetch h.licensePlate
            where h.reviewStatus = :status
               or (:status = com.placute.ocrbackend.model.DetectionReviewStatus.DE_REVIEW and h.reviewStatus is null)
            order by h.processedAt desc, h.id desc
            """)
    List<OcrHistory> findByReviewStatusWithLicensePlateOrderByProcessedAtDesc(
            @Param("status") DetectionReviewStatus status,
            Pageable pageable
    );

    @Query("""
            select h
            from OcrHistory h
            join fetch h.licensePlate lp
            where lower(lp.plateNumber) like lower(concat('%', :fragment, '%'))
            order by h.processedAt desc
            """)
    List<OcrHistory> findByPlateNumberContainingWithLicensePlateOrderByProcessedAtDesc(@Param("fragment") String fragment);

    @Query("""
            select h
            from OcrHistory h
            join fetch h.licensePlate lp
            where lp.plateNumber = :plateNumber
            order by h.processedAt desc
            """)
    List<OcrHistory> findByExactPlateNumberWithLicensePlateOrderByProcessedAtDesc(@Param("plateNumber") String plateNumber);

    Optional<OcrHistory> findTopByLicensePlate_IdOrderByProcessedAtDesc(Long licensePlateId);

    @Query("""
            select count(h)
            from OcrHistory h
            where h.reviewStatus = :status
               or (:status = com.placute.ocrbackend.model.DetectionReviewStatus.DE_REVIEW and h.reviewStatus is null)
            """)
    long countByReviewStatusOrNullForReview(@Param("status") DetectionReviewStatus status);
}
