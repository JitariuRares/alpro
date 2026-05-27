package com.placute.ocrbackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "video_detections",
        indexes = {
                @Index(name = "idx_video_detections_job_frame", columnList = "job_id, frame_index"),
                @Index(name = "idx_video_detections_plate_text", columnList = "plate_text")
        }
)
public class VideoDetection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private VideoJob job;

    @Column(name = "frame_index", nullable = false)
    private Integer frameIndex;

    @Column(name = "timestamp_ms")
    private Long timestampMs;

    @Column(name = "track_id")
    private Integer trackId;

    @Column(name = "plate_text", nullable = false, length = 32)
    private String plateText;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "bbox_x")
    private Integer bboxX;

    @Column(name = "bbox_y")
    private Integer bboxY;

    @Column(name = "bbox_w")
    private Integer bboxW;

    @Column(name = "bbox_h")
    private Integer bboxH;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", length = 32)
    private DetectionReviewStatus reviewStatus = DetectionReviewStatus.DE_REVIEW;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_reason", length = 500)
    private String reviewReason;

    @PrePersist
    public void prePersist() {
        if (reviewStatus == null) {
            reviewStatus = DetectionReviewStatus.DE_REVIEW;
        }
    }

    public Long getId() {
        return id;
    }

    public VideoJob getJob() {
        return job;
    }

    public void setJob(VideoJob job) {
        this.job = job;
    }

    public Integer getFrameIndex() {
        return frameIndex;
    }

    public void setFrameIndex(Integer frameIndex) {
        this.frameIndex = frameIndex;
    }

    public Long getTimestampMs() {
        return timestampMs;
    }

    public void setTimestampMs(Long timestampMs) {
        this.timestampMs = timestampMs;
    }

    public Integer getTrackId() {
        return trackId;
    }

    public void setTrackId(Integer trackId) {
        this.trackId = trackId;
    }

    public String getPlateText() {
        return plateText;
    }

    public void setPlateText(String plateText) {
        this.plateText = plateText;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Integer getBboxX() {
        return bboxX;
    }

    public void setBboxX(Integer bboxX) {
        this.bboxX = bboxX;
    }

    public Integer getBboxY() {
        return bboxY;
    }

    public void setBboxY(Integer bboxY) {
        this.bboxY = bboxY;
    }

    public Integer getBboxW() {
        return bboxW;
    }

    public void setBboxW(Integer bboxW) {
        this.bboxW = bboxW;
    }

    public Integer getBboxH() {
        return bboxH;
    }

    public void setBboxH(Integer bboxH) {
        this.bboxH = bboxH;
    }

    public DetectionReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(DetectionReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewReason() {
        return reviewReason;
    }

    public void setReviewReason(String reviewReason) {
        this.reviewReason = reviewReason;
    }
}
