package com.placute.ocrbackend.dto;

import com.placute.ocrbackend.model.DetectionReviewStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PoliceLookupDto {

    private String plateNumber;
    private PlateDto plate;
    private List<InsuranceDto> insurances;
    private List<ParkingDto> parkingHistory;
    private List<OcrDetectionDto> recentOcrDetections;
    private List<VideoDetectionDto> recentVideoDetections;
    private List<AuditEventDto> auditEvents;

    public PoliceLookupDto(
            String plateNumber,
            PlateDto plate,
            List<InsuranceDto> insurances,
            List<ParkingDto> parkingHistory,
            List<OcrDetectionDto> recentOcrDetections,
            List<VideoDetectionDto> recentVideoDetections,
            List<AuditEventDto> auditEvents
    ) {
        this.plateNumber = plateNumber;
        this.plate = plate;
        this.insurances = insurances;
        this.parkingHistory = parkingHistory;
        this.recentOcrDetections = recentOcrDetections;
        this.recentVideoDetections = recentVideoDetections;
        this.auditEvents = auditEvents;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public PlateDto getPlate() {
        return plate;
    }

    public List<InsuranceDto> getInsurances() {
        return insurances;
    }

    public List<ParkingDto> getParkingHistory() {
        return parkingHistory;
    }

    public List<OcrDetectionDto> getRecentOcrDetections() {
        return recentOcrDetections;
    }

    public List<VideoDetectionDto> getRecentVideoDetections() {
        return recentVideoDetections;
    }

    public List<AuditEventDto> getAuditEvents() {
        return auditEvents;
    }

    public static class PlateDto {
        private Long id;
        private String plateNumber;
        private String brand;
        private String model;
        private String owner;
        private String imagePath;
        private LocalDateTime detectedAt;
        private Double confidence;
        private BboxDto bbox;

        public PlateDto(
                Long id,
                String plateNumber,
                String brand,
                String model,
                String owner,
                String imagePath,
                LocalDateTime detectedAt,
                Double confidence,
                BboxDto bbox
        ) {
            this.id = id;
            this.plateNumber = plateNumber;
            this.brand = brand;
            this.model = model;
            this.owner = owner;
            this.imagePath = imagePath;
            this.detectedAt = detectedAt;
            this.confidence = confidence;
            this.bbox = bbox;
        }

        public Long getId() {
            return id;
        }

        public String getPlateNumber() {
            return plateNumber;
        }

        public String getBrand() {
            return brand;
        }

        public String getModel() {
            return model;
        }

        public String getOwner() {
            return owner;
        }

        public String getImagePath() {
            return imagePath;
        }

        public LocalDateTime getDetectedAt() {
            return detectedAt;
        }

        public Double getConfidence() {
            return confidence;
        }

        public BboxDto getBbox() {
            return bbox;
        }
    }

    public static class InsuranceDto {
        private Long id;
        private String company;
        private LocalDate validFrom;
        private LocalDate validTo;

        public InsuranceDto(Long id, String company, LocalDate validFrom, LocalDate validTo) {
            this.id = id;
            this.company = company;
            this.validFrom = validFrom;
            this.validTo = validTo;
        }

        public Long getId() {
            return id;
        }

        public String getCompany() {
            return company;
        }

        public LocalDate getValidFrom() {
            return validFrom;
        }

        public LocalDate getValidTo() {
            return validTo;
        }
    }

    public static class ParkingDto {
        private Long id;
        private String parkingZone;
        private LocalDateTime entryTime;
        private LocalDateTime exitTime;
        private String status;
        private Long durationMinutes;
        private boolean hasEntryImage;
        private boolean hasExitImage;

        public ParkingDto(
                Long id,
                String parkingZone,
                LocalDateTime entryTime,
                LocalDateTime exitTime,
                String status,
                Long durationMinutes,
                boolean hasEntryImage,
                boolean hasExitImage
        ) {
            this.id = id;
            this.parkingZone = parkingZone;
            this.entryTime = entryTime;
            this.exitTime = exitTime;
            this.status = status;
            this.durationMinutes = durationMinutes;
            this.hasEntryImage = hasEntryImage;
            this.hasExitImage = hasExitImage;
        }

        public Long getId() {
            return id;
        }

        public String getParkingZone() {
            return parkingZone;
        }

        public LocalDateTime getEntryTime() {
            return entryTime;
        }

        public LocalDateTime getExitTime() {
            return exitTime;
        }

        public String getStatus() {
            return status;
        }

        public Long getDurationMinutes() {
            return durationMinutes;
        }

        public boolean isHasEntryImage() {
            return hasEntryImage;
        }

        public boolean isHasExitImage() {
            return hasExitImage;
        }
    }

    public static class OcrDetectionDto {
        private Long id;
        private LocalDateTime processedAt;
        private Double confidence;
        private BboxDto bbox;
        private DetectionReviewStatus reviewStatus;
        private String reviewedBy;
        private String reviewReason;

        public OcrDetectionDto(
                Long id,
                LocalDateTime processedAt,
                Double confidence,
                BboxDto bbox,
                DetectionReviewStatus reviewStatus,
                String reviewedBy,
                String reviewReason
        ) {
            this.id = id;
            this.processedAt = processedAt;
            this.confidence = confidence;
            this.bbox = bbox;
            this.reviewStatus = reviewStatus;
            this.reviewedBy = reviewedBy;
            this.reviewReason = reviewReason;
        }

        public Long getId() {
            return id;
        }

        public LocalDateTime getProcessedAt() {
            return processedAt;
        }

        public Double getConfidence() {
            return confidence;
        }

        public BboxDto getBbox() {
            return bbox;
        }

        public DetectionReviewStatus getReviewStatus() {
            return reviewStatus;
        }

        public String getReviewedBy() {
            return reviewedBy;
        }

        public String getReviewReason() {
            return reviewReason;
        }
    }

    public static class VideoDetectionDto {
        private Long id;
        private Integer frameIndex;
        private Long timestampMs;
        private Integer trackId;
        private Double confidence;
        private BboxDto bbox;
        private DetectionReviewStatus reviewStatus;
        private String reviewedBy;
        private String reviewReason;

        public VideoDetectionDto(
                Long id,
                Integer frameIndex,
                Long timestampMs,
                Integer trackId,
                Double confidence,
                BboxDto bbox,
                DetectionReviewStatus reviewStatus,
                String reviewedBy,
                String reviewReason
        ) {
            this.id = id;
            this.frameIndex = frameIndex;
            this.timestampMs = timestampMs;
            this.trackId = trackId;
            this.confidence = confidence;
            this.bbox = bbox;
            this.reviewStatus = reviewStatus;
            this.reviewedBy = reviewedBy;
            this.reviewReason = reviewReason;
        }

        public Long getId() {
            return id;
        }

        public Integer getFrameIndex() {
            return frameIndex;
        }

        public Long getTimestampMs() {
            return timestampMs;
        }

        public Integer getTrackId() {
            return trackId;
        }

        public Double getConfidence() {
            return confidence;
        }

        public BboxDto getBbox() {
            return bbox;
        }

        public DetectionReviewStatus getReviewStatus() {
            return reviewStatus;
        }

        public String getReviewedBy() {
            return reviewedBy;
        }

        public String getReviewReason() {
            return reviewReason;
        }
    }

    public static class AuditEventDto {
        private Long id;
        private String actorUsername;
        private String action;
        private String details;
        private LocalDateTime createdAt;

        public AuditEventDto(Long id, String actorUsername, String action, String details, LocalDateTime createdAt) {
            this.id = id;
            this.actorUsername = actorUsername;
            this.action = action;
            this.details = details;
            this.createdAt = createdAt;
        }

        public Long getId() {
            return id;
        }

        public String getActorUsername() {
            return actorUsername;
        }

        public String getAction() {
            return action;
        }

        public String getDetails() {
            return details;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }

    public static class BboxDto {
        private Integer x;
        private Integer y;
        private Integer w;
        private Integer h;

        public BboxDto(Integer x, Integer y, Integer w, Integer h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public Integer getX() {
            return x;
        }

        public Integer getY() {
            return y;
        }

        public Integer getW() {
            return w;
        }

        public Integer getH() {
            return h;
        }
    }
}
