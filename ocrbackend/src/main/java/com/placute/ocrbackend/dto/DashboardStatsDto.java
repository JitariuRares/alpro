package com.placute.ocrbackend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class DashboardStatsDto {
    private long totalPlates;
    private long totalInsurances;
    private long totalParkings;
    private long activeInsurances;
    private long expiredInsurances;
    private long expiringInsurancesNext30Days;
    private long openParkings;
    private long pendingDetectionReviews;
    private long runningVideoJobs;
    private Map<String, Long> topCountiesLast7Days;
    private List<OpenParkingDto> recentOpenParkings;
    private List<VideoJobDto> recentVideoJobs;
    private List<AuditEventDto> recentAuditEvents;

    public DashboardStatsDto(long totalPlates, long totalInsurances, long totalParkings, Map<String, Long> topCountiesLast7Days) {
        this.totalPlates = totalPlates;
        this.totalInsurances = totalInsurances;
        this.totalParkings = totalParkings;
        this.topCountiesLast7Days = topCountiesLast7Days;
    }

    public DashboardStatsDto(
            long totalPlates,
            long totalInsurances,
            long totalParkings,
            long activeInsurances,
            long expiredInsurances,
            long expiringInsurancesNext30Days,
            long openParkings,
            long pendingDetectionReviews,
            long runningVideoJobs,
            Map<String, Long> topCountiesLast7Days,
            List<OpenParkingDto> recentOpenParkings,
            List<VideoJobDto> recentVideoJobs,
            List<AuditEventDto> recentAuditEvents
    ) {
        this.totalPlates = totalPlates;
        this.totalInsurances = totalInsurances;
        this.totalParkings = totalParkings;
        this.activeInsurances = activeInsurances;
        this.expiredInsurances = expiredInsurances;
        this.expiringInsurancesNext30Days = expiringInsurancesNext30Days;
        this.openParkings = openParkings;
        this.pendingDetectionReviews = pendingDetectionReviews;
        this.runningVideoJobs = runningVideoJobs;
        this.topCountiesLast7Days = topCountiesLast7Days;
        this.recentOpenParkings = recentOpenParkings;
        this.recentVideoJobs = recentVideoJobs;
        this.recentAuditEvents = recentAuditEvents;
    }

    public long getTotalPlates() {
        return totalPlates;
    }

    public long getTotalInsurances() {
        return totalInsurances;
    }

    public long getTotalParkings() {
        return totalParkings;
    }

    public long getActiveInsurances() {
        return activeInsurances;
    }

    public long getExpiredInsurances() {
        return expiredInsurances;
    }

    public long getExpiringInsurancesNext30Days() {
        return expiringInsurancesNext30Days;
    }

    public long getOpenParkings() {
        return openParkings;
    }

    public long getPendingDetectionReviews() {
        return pendingDetectionReviews;
    }

    public long getRunningVideoJobs() {
        return runningVideoJobs;
    }

    public Map<String, Long> getTopCountiesLast7Days() {
        return topCountiesLast7Days;
    }

    public List<OpenParkingDto> getRecentOpenParkings() {
        return recentOpenParkings;
    }

    public List<VideoJobDto> getRecentVideoJobs() {
        return recentVideoJobs;
    }

    public List<AuditEventDto> getRecentAuditEvents() {
        return recentAuditEvents;
    }

    public record OpenParkingDto(Long id, String plateNumber, String parkingZone, LocalDateTime entryTime) {
    }

    public record VideoJobDto(
            Long id,
            String sourceFilename,
            String status,
            Integer progressPercent,
            LocalDateTime createdAt,
            LocalDateTime completedAt
    ) {
    }

    public record AuditEventDto(
            Long id,
            String actorUsername,
            String action,
            String targetPlateNumber,
            String details,
            LocalDateTime createdAt
    ) {
    }
}
