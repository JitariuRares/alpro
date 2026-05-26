package com.placute.ocrbackend.service;

import com.placute.ocrbackend.dto.PoliceLookupDto;
import com.placute.ocrbackend.model.LicensePlate;
import com.placute.ocrbackend.model.OcrHistory;
import com.placute.ocrbackend.model.ParkingHistory;
import com.placute.ocrbackend.model.ParkingSessionStatus;
import com.placute.ocrbackend.model.VideoDetection;
import com.placute.ocrbackend.repository.InsuranceRepository;
import com.placute.ocrbackend.repository.LicensePlateRepository;
import com.placute.ocrbackend.repository.OcrHistoryRepository;
import com.placute.ocrbackend.repository.ParkingHistoryRepository;
import com.placute.ocrbackend.repository.VideoDetectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class PoliceLookupService {

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

    public PoliceLookupDto lookupByPlateNumber(String rawPlateNumber) {
        String plateNumber = normalizePlate(rawPlateNumber);
        if (plateNumber.isBlank()) {
            throw new RuntimeException("Numarul placutei este obligatoriu.");
        }

        List<LicensePlate> matchedPlates = licensePlateRepository.findByPlateNumber(plateNumber);
        Optional<LicensePlate> latestPlate = matchedPlates.stream()
                .max(Comparator
                        .comparing(LicensePlate::getDetectedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(LicensePlate::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        PoliceLookupDto.PlateDto plateDto = latestPlate
                .map(lp -> new PoliceLookupDto.PlateDto(
                        lp.getId(),
                        lp.getPlateNumber(),
                        lp.getBrand(),
                        lp.getModel(),
                        lp.getOwner(),
                        lp.getImagePath(),
                        lp.getDetectedAt(),
                        lp.getConfidence(),
                        bbox(lp.getBboxX(), lp.getBboxY(), lp.getBboxW(), lp.getBboxH())
                ))
                .orElse(null);

        List<PoliceLookupDto.InsuranceDto> insurances = insuranceRepository
                .findByLicensePlate_PlateNumberOrderByValidToDesc(plateNumber)
                .stream()
                .map(ins -> new PoliceLookupDto.InsuranceDto(
                        ins.getId(),
                        ins.getCompany(),
                        ins.getValidFrom(),
                        ins.getValidTo()
                ))
                .toList();

        List<PoliceLookupDto.ParkingDto> parkingHistory = parkingHistoryRepository
                .findTop20ByLicensePlate_PlateNumberOrderByEntryTimeDesc(plateNumber)
                .stream()
                .map(this::toParkingDto)
                .toList();

        List<PoliceLookupDto.OcrDetectionDto> recentOcrDetections = ocrHistoryRepository
                .findByExactPlateNumberWithLicensePlateOrderByProcessedAtDesc(plateNumber)
                .stream()
                .limit(20)
                .map(this::toOcrDetectionDto)
                .toList();

        List<PoliceLookupDto.VideoDetectionDto> recentVideoDetections = videoDetectionRepository
                .findTop100ByPlateTextOrderByIdDesc(plateNumber)
                .stream()
                .limit(50)
                .map(this::toVideoDetectionDto)
                .toList();

        return new PoliceLookupDto(
                plateNumber,
                plateDto,
                insurances,
                parkingHistory,
                recentOcrDetections,
                recentVideoDetections
        );
    }

    private PoliceLookupDto.OcrDetectionDto toOcrDetectionDto(OcrHistory history) {
        return new PoliceLookupDto.OcrDetectionDto(
                history.getId(),
                history.getProcessedAt(),
                history.getConfidence(),
                bbox(history.getBboxX(), history.getBboxY(), history.getBboxW(), history.getBboxH())
        );
    }

    private PoliceLookupDto.VideoDetectionDto toVideoDetectionDto(VideoDetection detection) {
        return new PoliceLookupDto.VideoDetectionDto(
                detection.getId(),
                detection.getFrameIndex(),
                detection.getTimestampMs(),
                detection.getTrackId(),
                detection.getConfidence(),
                bbox(detection.getBboxX(), detection.getBboxY(), detection.getBboxW(), detection.getBboxH())
        );
    }

    private PoliceLookupDto.ParkingDto toParkingDto(ParkingHistory parking) {
        ParkingSessionStatus status = parking.getStatus();
        if (status == null) {
            status = parking.getExitTime() == null ? ParkingSessionStatus.OPEN : ParkingSessionStatus.CLOSED;
        }

        Long durationMinutes = null;
        LocalDateTime entryTime = parking.getEntryTime();
        LocalDateTime effectiveEnd = parking.getExitTime() != null ? parking.getExitTime() : LocalDateTime.now();
        if (entryTime != null && !effectiveEnd.isBefore(entryTime)) {
            durationMinutes = Duration.between(entryTime, effectiveEnd).toMinutes();
        }

        return new PoliceLookupDto.ParkingDto(
                parking.getId(),
                parking.getEntryTime(),
                parking.getExitTime(),
                status.name(),
                durationMinutes,
                parking.getEntryImagePath() != null && !parking.getEntryImagePath().isBlank(),
                parking.getExitImagePath() != null && !parking.getExitImagePath().isBlank()
        );
    }

    private PoliceLookupDto.BboxDto bbox(Integer x, Integer y, Integer w, Integer h) {
        if (x == null && y == null && w == null && h == null) {
            return null;
        }
        return new PoliceLookupDto.BboxDto(x, y, w, h);
    }

    private String normalizePlate(String rawPlateNumber) {
        if (rawPlateNumber == null) {
            return "";
        }
        return rawPlateNumber.trim().toUpperCase(Locale.ROOT);
    }
}
