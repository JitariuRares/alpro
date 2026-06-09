package com.placute.ocrbackend.service;

import com.placute.ocrbackend.dto.DashboardStatsDto;
import com.placute.ocrbackend.model.DetectionReviewStatus;
import com.placute.ocrbackend.model.LicensePlate;
import com.placute.ocrbackend.model.ParkingSessionStatus;
import com.placute.ocrbackend.model.VideoJobStatus;
import com.placute.ocrbackend.repository.AuditLogRepository;
import com.placute.ocrbackend.repository.InsuranceRepository;
import com.placute.ocrbackend.repository.LicensePlateRepository;
import com.placute.ocrbackend.repository.OcrHistoryRepository;
import com.placute.ocrbackend.repository.ParkingHistoryRepository;
import com.placute.ocrbackend.repository.VideoDetectionRepository;
import com.placute.ocrbackend.repository.VideoJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private LicensePlateRepository licensePlateRepository;

    @Autowired
    private InsuranceRepository insuranceRepository;

    @Autowired
    private ParkingHistoryRepository parkingHistoryRepository;

    @Autowired
    private OcrHistoryRepository ocrHistoryRepository;

    @Autowired
    private VideoDetectionRepository videoDetectionRepository;

    @Autowired
    private VideoJobRepository videoJobRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    public DashboardStatsDto getDashboardStats() {
        long totalPlates = licensePlateRepository.count();
        long totalInsurances = insuranceRepository.count();
        long totalParkings = parkingHistoryRepository.count();
        long openParkings = parkingHistoryRepository.countByStatus(ParkingSessionStatus.OPEN);
        long pendingDetectionReviews =
                ocrHistoryRepository.countByReviewStatusOrNullForReview(DetectionReviewStatus.DE_REVIEW)
                        + videoDetectionRepository.countByReviewStatusOrNullForReview(DetectionReviewStatus.DE_REVIEW);
        long runningVideoJobs = videoJobRepository.countByStatus(VideoJobStatus.PENDING)
                + videoJobRepository.countByStatus(VideoJobStatus.RUNNING);

        LocalDate today = LocalDate.now();
        long activeInsurances = insuranceRepository.countByValidFromLessThanEqualAndValidToGreaterThanEqual(today, today);
        long expiredInsurances = insuranceRepository.countByValidToBefore(today);
        long expiringInsurancesNext30Days = insuranceRepository.countByValidToBetween(today, today.plusDays(30));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        List<LicensePlate> recentPlates = licensePlateRepository.findByDetectedAtAfter(sevenDaysAgo);

        Map<String, Long> countyCounts = recentPlates.stream()
                .map(lp -> extractCountyPrefix(lp.getPlateNumber()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        Map<String, Long> topCounties = countyCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        List<DashboardStatsDto.OpenParkingDto> recentOpenParkings = parkingHistoryRepository
                .findByStatusWithLicensePlateOrderByEntryTimeDesc(ParkingSessionStatus.OPEN, PageRequest.of(0, 5))
                .stream()
                .map(parking -> new DashboardStatsDto.OpenParkingDto(
                        parking.getId(),
                        parking.getLicensePlate() != null ? parking.getLicensePlate().getPlateNumber() : null,
                        parking.getParkingZone(),
                        parking.getEntryTime()
                ))
                .toList();

        List<DashboardStatsDto.VideoJobDto> recentVideoJobs = videoJobRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(job -> new DashboardStatsDto.VideoJobDto(
                        job.getId(),
                        job.getSourceFilename(),
                        job.getStatus() != null ? job.getStatus().name() : null,
                        job.getProgressPercent(),
                        job.getCreatedAt(),
                        job.getCompletedAt()
                ))
                .toList();

        List<DashboardStatsDto.AuditEventDto> recentAuditEvents = auditLogRepository.findTop6ByOrderByCreatedAtDescIdDesc()
                .stream()
                .map(log -> new DashboardStatsDto.AuditEventDto(
                        log.getId(),
                        log.getActorUsername(),
                        log.getAction(),
                        log.getTargetPlateNumber(),
                        log.getDetails(),
                        log.getCreatedAt()
                ))
                .toList();

        return new DashboardStatsDto(
                totalPlates,
                totalInsurances,
                totalParkings,
                activeInsurances,
                expiredInsurances,
                expiringInsurancesNext30Days,
                openParkings,
                pendingDetectionReviews,
                runningVideoJobs,
                topCounties,
                recentOpenParkings,
                recentVideoJobs,
                recentAuditEvents
        );
    }

    private String extractCountyPrefix(String plate) {
        if (plate == null || plate.length() < 2) return null;
        if (plate.matches("^[A-Z]{2}.*")) {
            return plate.substring(0, 2);
        } else if (plate.matches("^[A-Z]{1}.*")) {
            return plate.substring(0, 1);
        }
        return null;
    }
}
