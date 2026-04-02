package com.placute.ocrbackend.controller;

import com.placute.ocrbackend.dto.OcrPlateDto;
import com.placute.ocrbackend.model.LicensePlate;
import com.placute.ocrbackend.repository.LicensePlateRepository;
import com.placute.ocrbackend.service.OcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class OcrController {

    @Autowired
    private OcrService ocrService;

    @Autowired
    private LicensePlateRepository plateRepository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @PreAuthorize("hasAnyRole('POLICE', 'PARKING')")
    @PostMapping("/ocr")
    public ResponseEntity<String> uploadImage(@RequestParam("image") MultipartFile file) throws IOException {
        File convFile = toStoredUploadFile(file);
        String result = ocrService.recognizeText(convFile);
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAnyRole('POLICE', 'PARKING')")
    @PostMapping("/ocr/full")
    public ResponseEntity<?> uploadImageFull(@RequestParam("image") MultipartFile file) throws IOException {
        File convFile = toStoredUploadFile(file);

        OcrService.OcrDetectionResult detection = ocrService.recognizeAndReturnPlate(convFile);
        if (detection == null) {
            return ResponseEntity.status(404).body("Nicio placuta valida gasita.");
        }
        LicensePlate lp = detection.licensePlate();

        OcrPlateDto.BboxDto bboxDto = null;
        if (detection.bbox() != null) {
            bboxDto = new OcrPlateDto.BboxDto(
                    detection.bbox().getX(),
                    detection.bbox().getY(),
                    detection.bbox().getW(),
                    detection.bbox().getH()
            );
        }

        OcrPlateDto dto = new OcrPlateDto(
                lp.getId(),
                lp.getPlateNumber(),
                lp.getBrand(),
                lp.getModel(),
                lp.getOwner(),
                lp.getImagePath(),
                lp.getDetectedAt(),
                lp.getUser() != null ? lp.getUser().getUsername() : null,
                lp.getUser() != null ? lp.getUser().getRole().name() : null,
                detection.confidence(),
                bboxDto
        );
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasAnyRole('POLICE', 'PARKING')")
    @GetMapping("/plates")
    public ResponseEntity<List<LicensePlate>> getAllPlates() {
        List<LicensePlate> plates = plateRepository.findAll();
        return ResponseEntity.ok(plates);
    }

    private File toStoredUploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Fisierul incarcat este gol.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new RuntimeException("Se accepta doar fisiere imagine.");
        }

        String originalName = Objects.requireNonNullElse(file.getOriginalFilename(), "upload.bin");
        String suffix = resolveSuffix(originalName);
        String uniqueName = "alpro-upload-" + UUID.randomUUID() + suffix;

        Path uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadRoot);
        Path targetPath = uploadRoot.resolve(uniqueName).normalize();
        if (!targetPath.startsWith(uploadRoot)) {
            throw new IOException("Calea de upload este invalida.");
        }

        file.transferTo(targetPath);
        return targetPath.toFile();
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
}
