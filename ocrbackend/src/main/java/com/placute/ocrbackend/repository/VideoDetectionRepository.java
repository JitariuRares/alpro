package com.placute.ocrbackend.repository;

import com.placute.ocrbackend.model.VideoDetection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoDetectionRepository extends JpaRepository<VideoDetection, Long> {
    Page<VideoDetection> findByJob_IdOrderByFrameIndexAscIdAsc(Long jobId, Pageable pageable);
    long countByJob_Id(Long jobId);
    void deleteByJob_Id(Long jobId);
    List<VideoDetection> findTop100ByPlateTextOrderByIdDesc(String plateText);
}
