package com.placute.ocrbackend.service;

import com.placute.ocrbackend.integration.MlAlprClient;
import com.placute.ocrbackend.integration.OpenAIOcrService;
import com.placute.ocrbackend.integration.OpenAIVehicleAttributeService;
import com.placute.ocrbackend.integration.dto.MlAlprResult;
import com.placute.ocrbackend.dto.VehicleAttributesDto;
import com.placute.ocrbackend.model.LicensePlate;
import com.placute.ocrbackend.model.OcrHistory;
import com.placute.ocrbackend.model.PlateType;
import com.placute.ocrbackend.repository.LicensePlateRepository;
import com.placute.ocrbackend.repository.OcrHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class OcrService {

    @Autowired
    private LicensePlateRepository plateRepository;

    @Autowired
    private OcrHistoryRepository historyRepository;

    @Autowired
    private OpenAIOcrService openAIOcrService;

    @Autowired
    private MlAlprClient mlAlprClient;

    @Autowired
    private OpenAIVehicleAttributeService vehicleAttributeService;

    @Autowired
    private RomanianPlateValidator plateValidator;

    @Value("${alpr.fallback.openai.enabled:false}")
    private boolean openAiFallbackEnabled;

    public record OcrDetectionResult(LicensePlate licensePlate, Double confidence, MlAlprResult.Bbox bbox) {}

    private record PlateDetection(String plate, PlateType plateType, Double confidence, MlAlprResult.Bbox bbox) {}

    public String recognizeText(File imageFile) {
        try {
            PlateDetection detection = detectPlateWithFallback(imageFile);
            String plate = detection != null ? detection.plate() : null;

            if (plate == null) {
                return "Nicio placuta detectata.";
            }

            savePlate(detection, imageFile);
            return "Placuta detectata si salvata: " + plate;
        } catch (Exception e) {
            return "Eroare la OCR: " + e.getMessage();
        }
    }

    public OcrDetectionResult recognizeAndReturnPlate(File imageFile) {
        try {
            PlateDetection detection = detectPlateWithFallback(imageFile);
            String plate = detection != null ? detection.plate() : null;

            if (plate == null) {
                return null;
            }

            LicensePlate savedPlate = savePlate(detection, imageFile);
            enrichVehicleAttributes(savedPlate, imageFile);
            return new OcrDetectionResult(
                    savedPlate,
                    detection.confidence(),
                    detection.bbox()
            );
        } catch (Exception e) {
            throw new RuntimeException("Eroare la OCR: " + e.getMessage());
        }
    }

    private PlateDetection detectPlateWithFallback(File imageFile) {
        PlateDetection mlDetection = detectWithMlService(imageFile);
        if (mlDetection != null) {
            return mlDetection;
        }

        if (openAiFallbackEnabled) {
            System.out.println("ML service nu a detectat placuta. Folosim fallback OpenAI...");
            return detectWithOpenAI(imageFile);
        }

        return null;
    }

    private PlateDetection detectWithMlService(File imageFile) {
        try {
            MlAlprResult result = mlAlprClient.detectPlate(imageFile);
            if (result == null) {
                return null;
            }

            RomanianPlateValidator.ValidationResult directPlate = classifyPlate(result.getPlateText());
            if (directPlate != null) {
                return new PlateDetection(
                        directPlate.normalizedPlate(),
                        directPlate.plateType(),
                        result.getConfidence(),
                        result.getBbox()
                );
            }

            if (result.getCandidates() != null) {
                for (MlAlprResult.Candidate candidate : result.getCandidates()) {
                    RomanianPlateValidator.ValidationResult candidatePlate = classifyPlate(candidate.getText());
                    if (candidatePlate != null) {
                        return new PlateDetection(
                                candidatePlate.normalizedPlate(),
                                candidatePlate.plateType(),
                                candidate.getConfidence(),
                                result.getBbox()
                        );
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Eroare la apel ML service: " + e.getMessage());
        }

        return null;
    }

    private PlateDetection detectWithOpenAI(File image) {
        try {
            RomanianPlateValidator.ValidationResult plate = classifyPlate(openAIOcrService.detectPlateNumber(image));
            if (plate == null) {
                return null;
            }
            return new PlateDetection(plate.normalizedPlate(), plate.plateType(), null, null);
        } catch (IOException e) {
            System.out.println("Eroare OpenAI: " + e.getMessage());
            return null;
        }
    }

    private String extractPlate(String rawText) {
        RomanianPlateValidator.ValidationResult result = classifyPlate(rawText);
        return result != null ? result.normalizedPlate() : null;
    }

    private RomanianPlateValidator.ValidationResult classifyPlate(String rawText) {
        RomanianPlateValidator.ValidationResult result = plateValidator.validate(rawText);
        return result.plateType() == PlateType.UNKNOWN ? null : result;
    }

    private LicensePlate savePlate(PlateDetection detection, File imageFile) {
        List<LicensePlate> existing = plateRepository.findByPlateNumber(detection.plate());
        LicensePlate lp;
        if (existing.isEmpty()) {
            lp = new LicensePlate(detection.plate(), imageFile.getAbsolutePath());
        } else {
            lp = existing.get(0);
        }

        lp.setDetectedAt(LocalDateTime.now());
        lp.setImagePath(imageFile.getAbsolutePath());
        lp.setConfidence(detection.confidence());
        lp.setPlateType(detection.plateType());

        MlAlprResult.Bbox bbox = detection.bbox();
        if (bbox != null) {
            lp.setBboxX(bbox.getX());
            lp.setBboxY(bbox.getY());
            lp.setBboxW(bbox.getW());
            lp.setBboxH(bbox.getH());
        } else {
            lp.setBboxX(null);
            lp.setBboxY(null);
            lp.setBboxW(null);
            lp.setBboxH(null);
        }

        LicensePlate saved = plateRepository.save(lp);

        String imageHash = computeSha256(imageFile);
        LocalDateTime processedAt = LocalDateTime.now();
        Optional<OcrHistory> existingHistory = historyRepository
                .findTopByLicensePlate_IdOrderByProcessedAtDesc(saved.getId());

        OcrHistory history = existingHistory.orElseGet(() -> new OcrHistory(
                saved,
                imageFile.getName(),
                imageHash,
                processedAt,
                detection.confidence(),
                saved.getBboxX(),
                saved.getBboxY(),
                saved.getBboxW(),
                saved.getBboxH()
        ));

        history.setLicensePlate(saved);
        history.setFilename(imageFile.getName());
        history.setImageHash(imageHash);
        history.setProcessedAt(processedAt);
        history.setConfidence(detection.confidence());
        history.setBboxX(saved.getBboxX());
        history.setBboxY(saved.getBboxY());
        history.setBboxW(saved.getBboxW());
        history.setBboxH(saved.getBboxH());
        historyRepository.save(history);

        return saved;
    }

    private void enrichVehicleAttributes(LicensePlate licensePlate, File imageFile) {
        try {
            Optional<VehicleAttributesDto> attributes = vehicleAttributeService.analyze(imageFile);
            if (attributes.isEmpty()) {
                return;
            }

            VehicleAttributesDto dto = attributes.get();
            licensePlate.setAiMakeSuggestion(dto.getMake());
            licensePlate.setAiModelSuggestion(dto.getModel());
            licensePlate.setAiColorSuggestion(dto.getColor());
            licensePlate.setAiBodyTypeSuggestion(dto.getBodyType());
            licensePlate.setAiVehicleConfidence(dto.getConfidence());
            licensePlate.setAiVehicleReasoning(dto.getReasoning());
            licensePlate.setAiVehicleAnalyzedAt(LocalDateTime.now());
            plateRepository.save(licensePlate);
        } catch (Exception e) {
            System.out.println("Analiza AI a vehiculului a fost omisa: " + e.getMessage());
        }
    }

    private String computeSha256(File file) {
        try {
            byte[] content = Files.readAllBytes(file.toPath());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(content);
            StringBuilder sb = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            System.out.println("Nu s-a putut calcula hash-ul imaginii: " + e.getMessage());
            return null;
        }
    }
}
