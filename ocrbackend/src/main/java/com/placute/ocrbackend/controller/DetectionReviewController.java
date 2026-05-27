package com.placute.ocrbackend.controller;

import com.placute.ocrbackend.dto.DetectionReviewItemDto;
import com.placute.ocrbackend.dto.DetectionReviewUpdateRequest;
import com.placute.ocrbackend.model.DetectionReviewStatus;
import com.placute.ocrbackend.model.OcrHistory;
import com.placute.ocrbackend.model.VideoDetection;
import com.placute.ocrbackend.repository.OcrHistoryRepository;
import com.placute.ocrbackend.repository.VideoDetectionRepository;
import com.placute.ocrbackend.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/detection-review")
public class DetectionReviewController {

    @Autowired
    private OcrHistoryRepository ocrHistoryRepository;

    @Autowired
    private VideoDetectionRepository videoDetectionRepository;

    @Autowired
    private AuditLogService auditLogService;

    @PreAuthorize("hasRole('POLICE')")
    @GetMapping
    public ResponseEntity<List<DetectionReviewItemDto>> getReviewQueue(
            @RequestParam(value = "status", required = false) DetectionReviewStatus status,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        DetectionReviewStatus normalizedStatus = Objects.requireNonNullElse(status, DetectionReviewStatus.DE_REVIEW);
        int normalizedLimit = normalizeLimit(limit);
        int perSourceLimit = Math.max(1, normalizedLimit);

        List<DetectionReviewItemDto> photoItems = ocrHistoryRepository
                .findByReviewStatusWithLicensePlateOrderByProcessedAtDesc(
                        normalizedStatus,
                        PageRequest.of(0, perSourceLimit)
                )
                .stream()
                .map(this::toPhotoDto)
                .toList();

        int rawVideoLimit = Math.min(2000, Math.max(100, normalizedLimit * 50));
        List<DetectionReviewItemDto> videoItems = groupVideoDetections(videoDetectionRepository
                .findByReviewStatusWithJobOrderByIdDesc(
                        normalizedStatus,
                        PageRequest.of(0, rawVideoLimit)
                ));

        List<DetectionReviewItemDto> items = Stream.concat(photoItems.stream(), videoItems.stream())
                .sorted(Comparator.comparing(
                        DetectionReviewItemDto::detectedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(normalizedLimit)
                .toList();

        return ResponseEntity.ok(items);
    }

    @PreAuthorize("hasRole('POLICE')")
    @PatchMapping("/{sourceType}/{sourceId}")
    public ResponseEntity<DetectionReviewItemDto> updateReviewStatus(
            @PathVariable String sourceType,
            @PathVariable Long sourceId,
            @RequestBody DetectionReviewUpdateRequest request,
            Authentication authentication
    ) {
        DetectionReviewStatus nextStatus = validateReviewStatus(request);
        String reviewReason = normalizeReviewReason(request.reviewReason(), nextStatus);
        String normalizedSourceType = sourceType.trim().toLowerCase(Locale.ROOT);

        if ("foto".equals(normalizedSourceType) || "photo".equals(normalizedSourceType)) {
            OcrHistory history = ocrHistoryRepository.findByIdWithLicensePlate(sourceId)
                    .orElseThrow(() -> new RuntimeException("Detectia foto nu exista."));
            applyReview(history, nextStatus, reviewReason, authentication);
            OcrHistory saved = ocrHistoryRepository.save(history);
            OcrHistory reloaded = ocrHistoryRepository.findByIdWithLicensePlate(saved.getId())
                    .orElseThrow(() -> new RuntimeException("Detectia foto nu exista."));
            auditReview(
                    authentication,
                    nextStatus,
                    reloaded.getLicensePlate().getPlateNumber(),
                    reviewDetail("Foto OCR #" + reloaded.getId(), reviewReason)
            );
            return ResponseEntity.ok(toPhotoDto(reloaded));
        }

        if ("video".equals(normalizedSourceType)) {
            String plateNumber = normalizePlateNumber(request.plateNumber());
            List<VideoDetection> detections = videoDetectionRepository.findByJobIdAndPlateTextWithJob(sourceId, plateNumber);
            if (detections.isEmpty()) {
                throw new RuntimeException("Grupul video nu exista.");
            }

            detections.forEach(detection -> applyReview(detection, nextStatus, reviewReason, authentication));
            videoDetectionRepository.saveAll(detections);

            List<VideoDetection> reloaded = videoDetectionRepository.findByJobIdAndPlateTextWithJob(sourceId, plateNumber);
            auditReview(
                    authentication,
                    nextStatus,
                    plateNumber,
                    reviewDetail("Video #" + sourceId + " - " + reloaded.size() + " aparitii", reviewReason)
            );
            return ResponseEntity.ok(toVideoGroupDto(new VideoReviewGroup(sourceId, plateNumber, reloaded)));
        }

        throw new RuntimeException("Tip detectie necunoscut.");
    }

    private void applyReview(OcrHistory history, DetectionReviewStatus status, String reviewReason, Authentication authentication) {
        history.setReviewStatus(status);
        history.setReviewedBy(authentication.getName());
        history.setReviewedAt(LocalDateTime.now());
        history.setReviewReason(reviewReason);
    }

    private void applyReview(VideoDetection detection, DetectionReviewStatus status, String reviewReason, Authentication authentication) {
        detection.setReviewStatus(status);
        detection.setReviewedBy(authentication.getName());
        detection.setReviewedAt(LocalDateTime.now());
        detection.setReviewReason(reviewReason);
    }

    private DetectionReviewStatus validateReviewStatus(DetectionReviewUpdateRequest request) {
        if (request == null || request.status() == null) {
            throw new RuntimeException("Status review lipsa.");
        }
        return request.status();
    }

    private String normalizeReviewReason(String reviewReason, DetectionReviewStatus status) {
        String normalizedReason = reviewReason == null ? null : reviewReason.trim();
        if (status == DetectionReviewStatus.REJECTED && (normalizedReason == null || normalizedReason.isBlank())) {
            throw new RuntimeException("Motivul respingerii este obligatoriu.");
        }
        if (normalizedReason == null || normalizedReason.isBlank()) {
            return null;
        }
        return normalizedReason.length() > 500 ? normalizedReason.substring(0, 500) : normalizedReason;
    }

    private String reviewDetail(String baseDetail, String reviewReason) {
        if (reviewReason == null || reviewReason.isBlank()) {
            return baseDetail;
        }
        return baseDetail + " | Motiv: " + reviewReason;
    }

    private void auditReview(Authentication authentication, DetectionReviewStatus status, String plateNumber, String detail) {
        String action = switch (status) {
            case CONFIRMED -> "DETECTION_CONFIRMED";
            case REJECTED -> "DETECTION_REJECTED";
            case DE_REVIEW -> "DETECTION_REOPENED";
        };
        auditLogService.log(authentication, action, plateNumber, detail);
    }

    private DetectionReviewItemDto toPhotoDto(OcrHistory history) {
        var plate = history.getLicensePlate();
        String detail = Stream.of(plate.getBrand(), plate.getModel(), plate.getOwner())
                .filter(value -> value != null && !value.isBlank())
                .reduce((first, second) -> first + " / " + second)
                .orElse("Fara detalii vehicul");

        return new DetectionReviewItemDto(
                "foto",
                history.getId(),
                plate.getPlateNumber(),
                history.getConfidence(),
                1L,
                history.getProcessedAt(),
                "Foto OCR",
                detail,
                normalizeStatus(history.getReviewStatus()),
                history.getReviewedBy(),
                history.getReviewedAt(),
                history.getReviewReason()
        );
    }

    private List<DetectionReviewItemDto> groupVideoDetections(List<VideoDetection> detections) {
        Map<String, VideoReviewGroup> groups = new LinkedHashMap<>();

        for (VideoDetection detection : detections) {
            String key = detection.getJob().getId() + "::" + detection.getPlateText();
            groups.computeIfAbsent(
                    key,
                    ignored -> new VideoReviewGroup(detection.getJob().getId(), detection.getPlateText(), new java.util.ArrayList<>())
            ).detections().add(detection);
        }

        return groups.values().stream()
                .map(this::toVideoGroupDto)
                .toList();
    }

    private DetectionReviewItemDto toVideoGroupDto(VideoReviewGroup group) {
        VideoDetection representative = group.detections().getFirst();
        LocalDateTime detectedAt = representative.getJob().getCompletedAt() != null
                ? representative.getJob().getCompletedAt()
                : representative.getJob().getCreatedAt();

        double maxConfidence = group.detections().stream()
                .map(VideoDetection::getConfidence)
                .filter(Objects::nonNull)
                .max(Double::compareTo)
                .orElse(0d);

        double avgConfidence = group.detections().stream()
                .map(VideoDetection::getConfidence)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0d);

        String detail = "%d aparitii brute / confidence mediu %.1f%%".formatted(
                group.detections().size(),
                avgConfidence * 100
        );

        String reviewedBy = group.detections().stream()
                .map(VideoDetection::getReviewedBy)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        LocalDateTime reviewedAt = group.detections().stream()
                .map(VideoDetection::getReviewedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        String reviewReason = group.detections().stream()
                .map(VideoDetection::getReviewReason)
                .filter(Objects::nonNull)
                .filter(reason -> !reason.isBlank())
                .findFirst()
                .orElse(null);

        return new DetectionReviewItemDto(
                "video",
                group.jobId(),
                group.plateText(),
                maxConfidence,
                (long) group.detections().size(),
                detectedAt,
                "Video #" + group.jobId(),
                detail,
                normalizeStatus(representative.getReviewStatus()),
                reviewedBy,
                reviewedAt,
                reviewReason
        );
    }

    private DetectionReviewStatus normalizeStatus(DetectionReviewStatus status) {
        return Objects.requireNonNullElse(status, DetectionReviewStatus.DE_REVIEW);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 100;
        }
        return Math.min(limit, 300);
    }

    private String normalizePlateNumber(String plateNumber) {
        if (plateNumber == null || plateNumber.trim().isBlank()) {
            throw new RuntimeException("Placuta lipsa pentru grupul video.");
        }
        return plateNumber.trim().toUpperCase(Locale.ROOT);
    }

    private record VideoReviewGroup(Long jobId, String plateText, List<VideoDetection> detections) {
    }
}
