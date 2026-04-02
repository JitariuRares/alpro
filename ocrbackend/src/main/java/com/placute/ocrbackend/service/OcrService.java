package com.placute.ocrbackend.service;

import com.placute.ocrbackend.integration.MlAlprClient;
import com.placute.ocrbackend.integration.OpenAIOcrService;
import com.placute.ocrbackend.integration.dto.MlAlprResult;
import com.placute.ocrbackend.model.LicensePlate;
import com.placute.ocrbackend.model.OcrHistory;
import com.placute.ocrbackend.repository.LicensePlateRepository;
import com.placute.ocrbackend.repository.OcrHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrService {

    private static final Pattern PLATE_PATTERN = Pattern.compile("[A-Z]{1,2}\\s?\\d{2}\\s?[A-Z]{3}");

    @Autowired
    private LicensePlateRepository plateRepository;

    @Autowired
    private OcrHistoryRepository historyRepository;

    @Autowired
    private OpenAIOcrService openAIOcrService;

    @Autowired
    private MlAlprClient mlAlprClient;

    @Value("${alpr.fallback.openai.enabled:false}")
    private boolean openAiFallbackEnabled;

    public record OcrDetectionResult(LicensePlate licensePlate, Double confidence, MlAlprResult.Bbox bbox) {}

    private record PlateDetection(String plate, Double confidence, MlAlprResult.Bbox bbox) {}

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

            String directPlate = extractPlate(result.getPlateText());
            if (directPlate != null) {
                return new PlateDetection(directPlate, result.getConfidence(), result.getBbox());
            }

            if (result.getCandidates() != null) {
                for (MlAlprResult.Candidate candidate : result.getCandidates()) {
                    String candidatePlate = extractPlate(candidate.getText());
                    if (candidatePlate != null) {
                        return new PlateDetection(candidatePlate, candidate.getConfidence(), result.getBbox());
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
            String plate = extractPlate(openAIOcrService.detectPlateNumber(image));
            if (plate == null) {
                return null;
            }
            return new PlateDetection(plate, null, null);
        } catch (IOException e) {
            System.out.println("Eroare OpenAI: " + e.getMessage());
            return null;
        }
    }

    private String extractPlate(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }

        String cleanedText = rawText.toUpperCase()
                .replaceAll("[^A-Z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        Matcher matcher = PLATE_PATTERN.matcher(cleanedText);
        if (matcher.find()) {
            return matcher.group().replaceAll("\\s+", "");
        }

        String compact = cleanedText.replaceAll("\\s+", "");
        if (compact.matches("[A-Z]{1,2}\\d{2}[A-Z]{3}")) {
            return compact;
        }

        return null;
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

        OcrHistory history = new OcrHistory(
                saved,
                imageFile.getName(),
                LocalDateTime.now(),
                detection.confidence(),
                saved.getBboxX(),
                saved.getBboxY(),
                saved.getBboxW(),
                saved.getBboxH()
        );
        historyRepository.save(history);

        return saved;
    }
}
