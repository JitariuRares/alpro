package com.placute.ocrbackend.repository;

import com.placute.ocrbackend.model.VideoJob;
import com.placute.ocrbackend.model.VideoJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoJobRepository extends JpaRepository<VideoJob, Long> {
    List<VideoJob> findAllByUser_IdOrderByCreatedAtDesc(Long userId);
    Optional<VideoJob> findByIdAndUser_Id(Long jobId, Long userId);
    long countByStatus(VideoJobStatus status);
    List<VideoJob> findTop5ByOrderByCreatedAtDesc();
}
