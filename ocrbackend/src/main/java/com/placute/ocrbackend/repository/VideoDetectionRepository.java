package com.placute.ocrbackend.repository;

import com.placute.ocrbackend.model.DetectionReviewStatus;
import com.placute.ocrbackend.model.VideoDetection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoDetectionRepository extends JpaRepository<VideoDetection, Long> {
    Page<VideoDetection> findByJob_IdOrderByFrameIndexAscIdAsc(Long jobId, Pageable pageable);
    long countByJob_Id(Long jobId);
    void deleteByJob_Id(Long jobId);
    List<VideoDetection> findTop100ByPlateTextOrderByIdDesc(String plateText);

    @Query("select d from VideoDetection d join fetch d.job where d.id = :id")
    Optional<VideoDetection> findByIdWithJob(@Param("id") Long id);

    @Query("""
            select d
            from VideoDetection d
            join fetch d.job
            where d.plateText = :plateText
            order by d.id desc
            """)
    List<VideoDetection> findTop100ByPlateTextWithJobOrderByIdDesc(@Param("plateText") String plateText, Pageable pageable);

    @Query("""
            select d
            from VideoDetection d
            join fetch d.job
            where d.job.id = :jobId
              and d.plateText = :plateText
              and (
                    d.reviewStatus = :status
                    or (:status = com.placute.ocrbackend.model.DetectionReviewStatus.DE_REVIEW and d.reviewStatus is null)
                  )
            order by d.id desc
            """)
    List<VideoDetection> findByJobIdAndPlateTextAndReviewStatusWithJob(
            @Param("jobId") Long jobId,
            @Param("plateText") String plateText,
            @Param("status") DetectionReviewStatus status
    );

    @Query("""
            select d
            from VideoDetection d
            join fetch d.job
            where d.job.id = :jobId
              and d.plateText = :plateText
            order by d.id desc
            """)
    List<VideoDetection> findByJobIdAndPlateTextWithJob(
            @Param("jobId") Long jobId,
            @Param("plateText") String plateText
    );

    @Query("""
            select d
            from VideoDetection d
            join fetch d.job
            where d.reviewStatus = :status
               or (:status = com.placute.ocrbackend.model.DetectionReviewStatus.DE_REVIEW and d.reviewStatus is null)
            order by d.id desc
            """)
    List<VideoDetection> findByReviewStatusWithJobOrderByIdDesc(
            @Param("status") DetectionReviewStatus status,
            Pageable pageable
    );

    @Query("""
            select count(d)
            from VideoDetection d
            where d.reviewStatus = :status
               or (:status = com.placute.ocrbackend.model.DetectionReviewStatus.DE_REVIEW and d.reviewStatus is null)
            """)
    long countByReviewStatusOrNullForReview(@Param("status") DetectionReviewStatus status);
}
