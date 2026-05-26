package com.placute.ocrbackend.service;

import com.placute.ocrbackend.dto.ParkingSessionDto;
import com.placute.ocrbackend.model.LicensePlate;
import com.placute.ocrbackend.model.ParkingHistory;
import com.placute.ocrbackend.model.ParkingSessionStatus;
import com.placute.ocrbackend.repository.LicensePlateRepository;
import com.placute.ocrbackend.repository.ParkingHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class ParkingService {

    @Autowired
    private ParkingHistoryRepository parkingHistoryRepository;

    @Autowired
    private LicensePlateRepository licensePlateRepository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public ParkingSessionDto registerEntry(String rawPlateNumber, LocalDateTime entryTime, MultipartFile image) throws IOException {
        String plateNumber = normalizePlate(rawPlateNumber);
        if (plateNumber.isBlank()) {
            throw new RuntimeException("Numarul placutei este obligatoriu.");
        }
        if (image == null || image.isEmpty()) {
            throw new RuntimeException("Dovada foto pentru ENTRY este obligatorie.");
        }

        LicensePlate plate = resolvePlate(plateNumber);
        boolean alreadyOpen = parkingHistoryRepository
                .findTopByLicensePlate_PlateNumberAndStatusOrderByEntryTimeDesc(plateNumber, ParkingSessionStatus.OPEN)
                .isPresent();
        if (alreadyOpen) {
            throw new RuntimeException("Exista deja o sesiune OPEN pentru aceasta placuta.");
        }

        String entryImagePath = storeEvidenceImage(image, "entry");

        ParkingHistory session = new ParkingHistory();
        session.setLicensePlate(plate);
        session.setEntryTime(entryTime != null ? entryTime : LocalDateTime.now());
        session.setExitTime(null);
        session.setEntryImagePath(entryImagePath);
        session.setExitImagePath(null);
        session.setStatus(ParkingSessionStatus.OPEN);

        ParkingHistory saved = parkingHistoryRepository.save(session);
        return toDto(saved);
    }

    public ParkingSessionDto registerExit(String rawPlateNumber, LocalDateTime exitTime, MultipartFile image) throws IOException {
        String plateNumber = normalizePlate(rawPlateNumber);
        if (plateNumber.isBlank()) {
            throw new RuntimeException("Numarul placutei este obligatoriu.");
        }
        if (image == null || image.isEmpty()) {
            throw new RuntimeException("Dovada foto pentru EXIT este obligatorie.");
        }

        ParkingHistory openSession = parkingHistoryRepository
                .findTopByLicensePlate_PlateNumberAndStatusOrderByEntryTimeDesc(plateNumber, ParkingSessionStatus.OPEN)
                .orElseThrow(() -> new RuntimeException("Nu exista sesiune OPEN pentru aceasta placuta."));

        LocalDateTime effectiveExitTime = exitTime != null ? exitTime : LocalDateTime.now();
        if (openSession.getEntryTime() != null && effectiveExitTime.isBefore(openSession.getEntryTime())) {
            throw new RuntimeException("EXIT nu poate fi inainte de ENTRY.");
        }

        String exitImagePath = storeEvidenceImage(image, "exit");
        openSession.setExitTime(effectiveExitTime);
        openSession.setExitImagePath(exitImagePath);
        openSession.setStatus(ParkingSessionStatus.CLOSED);

        ParkingHistory saved = parkingHistoryRepository.save(openSession);
        return toDto(saved);
    }

    public List<ParkingSessionDto> getSessionsByPlate(String rawPlateNumber) {
        String plateNumber = normalizePlate(rawPlateNumber);
        if (plateNumber.isBlank()) {
            throw new RuntimeException("Numarul placutei este obligatoriu.");
        }

        return parkingHistoryRepository.findByLicensePlate_PlateNumberOrderByEntryTimeDesc(plateNumber)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public ParkingEvidencePayload getEvidence(Long sessionId, String evidenceType) throws IOException {
        ParkingHistory session = parkingHistoryRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesiunea parking nu exista."));

        String normalizedType = evidenceType == null ? "" : evidenceType.trim().toLowerCase(Locale.ROOT);
        String selectedPath;
        if ("entry".equals(normalizedType)) {
            selectedPath = session.getEntryImagePath();
        } else if ("exit".equals(normalizedType)) {
            selectedPath = session.getExitImagePath();
        } else {
            throw new RuntimeException("Tip dovada invalid. Foloseste 'entry' sau 'exit'.");
        }

        if (selectedPath == null || selectedPath.isBlank()) {
            throw new RuntimeException("Dovada foto nu exista pentru tipul solicitat.");
        }

        Path uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        Path path = Path.of(selectedPath).toAbsolutePath().normalize();
        if (!path.startsWith(uploadRoot)) {
            throw new RuntimeException("Calea dovezii foto este invalida.");
        }
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new RuntimeException("Fisierul dovezii foto nu exista.");
        }

        Resource resource = toUrlResource(path);
        String contentType = Files.probeContentType(path);
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return new ParkingEvidencePayload(resource, contentType, Files.size(path));
    }

    private LicensePlate resolvePlate(String plateNumber) {
        return licensePlateRepository.findByPlateNumber(plateNumber)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Placuta de inmatriculare nu exista in baza de date."));
    }

    private String storeEvidenceImage(MultipartFile file, String prefix) throws IOException {
        validateImageFile(file);

        String originalName = Objects.requireNonNullElse(file.getOriginalFilename(), "parking-proof.bin");
        String suffix = resolveSuffix(originalName);
        String uniqueName = "parking-" + prefix + "-" + UUID.randomUUID() + suffix;

        Path uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        Path proofRoot = uploadRoot.resolve("parking-proofs").normalize();
        Files.createDirectories(proofRoot);

        Path targetPath = proofRoot.resolve(uniqueName).normalize();
        if (!targetPath.startsWith(proofRoot)) {
            throw new IOException("Calea dovezii foto este invalida.");
        }

        file.transferTo(targetPath);
        return targetPath.toString();
    }

    private void validateImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new RuntimeException("Se accepta doar fisiere imagine pentru dovada foto.");
        }
    }

    private String resolveSuffix(String originalName) {
        int lastDot = originalName.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < originalName.length() - 1) {
            String extension = originalName.substring(lastDot).toLowerCase(Locale.ROOT);
            if (extension.matches("^\\.[a-z0-9]{1,8}$")) {
                return extension;
            }
        }
        return ".bin";
    }

    private ParkingSessionDto toDto(ParkingHistory session) {
        ParkingSessionStatus normalizedStatus = session.getStatus();
        if (normalizedStatus == null) {
            normalizedStatus = session.getExitTime() == null ? ParkingSessionStatus.OPEN : ParkingSessionStatus.CLOSED;
        }

        Long durationMinutes = null;
        LocalDateTime entryTime = session.getEntryTime();
        LocalDateTime endTime = session.getExitTime();
        if (entryTime != null) {
            LocalDateTime effectiveEndTime = endTime != null ? endTime : LocalDateTime.now();
            if (!effectiveEndTime.isBefore(entryTime)) {
                durationMinutes = Duration.between(entryTime, effectiveEndTime).toMinutes();
            }
        }

        return new ParkingSessionDto(
                session.getId(),
                session.getLicensePlate() != null ? session.getLicensePlate().getPlateNumber() : null,
                session.getEntryTime(),
                session.getExitTime(),
                normalizedStatus,
                durationMinutes,
                session.getEntryImagePath() != null && !session.getEntryImagePath().isBlank(),
                session.getExitImagePath() != null && !session.getExitImagePath().isBlank()
        );
    }

    private String normalizePlate(String rawPlateNumber) {
        if (rawPlateNumber == null) {
            return "";
        }
        return rawPlateNumber.trim().toUpperCase(Locale.ROOT);
    }

    private Resource toUrlResource(Path path) {
        try {
            return new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Nu s-a putut accesa dovada foto.");
        }
    }

    public record ParkingEvidencePayload(Resource resource, String contentType, long contentLength) {
    }
}
