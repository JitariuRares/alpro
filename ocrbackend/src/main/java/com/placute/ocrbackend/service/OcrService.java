package com.placute.ocrbackend.service;

import com.placute.ocrbackend.integration.OpenAIOcrService;
import com.placute.ocrbackend.model.LicensePlate;
import com.placute.ocrbackend.model.OcrHistory;
import com.placute.ocrbackend.repository.LicensePlateRepository;
import com.placute.ocrbackend.repository.OcrHistoryRepository;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrService {

    @Autowired
    private LicensePlateRepository plateRepository;

    @Autowired
    private OcrHistoryRepository historyRepository;

    @Autowired
    private OpenAIOcrService openAIOcrService;

    @Value("${tesseract.datapath:C:\\Program Files\\Tesseract-OCR\\tessdata}")
    private String tesseractDataPath;

    private File preprocessImage(File inputFile) throws IOException {
        BufferedImage image = ImageIO.read(inputFile);

        BufferedImage gray = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        for (int y = 0; y < gray.getHeight(); y++) {
            for (int x = 0; x < gray.getWidth(); x++) {
                int rgb = gray.getRGB(x, y) & 0xFF;
                int enhanced = Math.min(255, (int) (rgb * 1.4));
                int pixel = (enhanced << 16) | (enhanced << 8) | enhanced;
                gray.setRGB(x, y, pixel);
            }
        }

        File output = new File(System.getProperty("java.io.tmpdir"), "preprocessed_advanced.png");
        ImageIO.write(gray, "png", output);
        return output;
    }

    public String recognizeText(File imageFile) {
        try {
            File processed = preprocessImage(imageFile);
            String plate = detectPlateWithFallback(processed);

            if (plate == null) {
                return "Nicio placuta detectata.";
            }

            savePlate(plate, imageFile);
            return "Placuta detectata si salvata: " + plate;

        } catch (Exception e) {
            return "Eroare la OCR: " + e.getMessage();
        }
    }

    public LicensePlate recognizeAndReturnPlate(File imageFile) {
        try {
            File processed = preprocessImage(imageFile);
            String plate = detectPlateWithFallback(processed);

            if (plate == null) {
                return null;
            }

            return savePlate(plate, imageFile);

        } catch (Exception e) {
            throw new RuntimeException("Eroare la OCR: " + e.getMessage());
        }
    }

    private String detectPlateWithFallback(File processedImage) {
        String plate = null;

        try {
            plate = detectWithTesseract(processedImage);
        } catch (Throwable t) {
            System.out.println("Tesseract indisponibil. Fallback OpenAI: " + t.getMessage());
        }

        if (plate == null) {
            System.out.println("Tesseract a esuat. Apelam OpenAI...");
            plate = detectWithOpenAI(processedImage);
        }

        return plate;
    }

    private String detectWithTesseract(File image) throws TesseractException {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tesseractDataPath);
        tesseract.setLanguage("eng");

        String rawText = tesseract.doOCR(image);
        System.out.println("TEXT DETECTAT de Tesseract:\n" + rawText);
        String cleanedText = rawText.toUpperCase().replaceAll("[^A-Z0-9 ]", "");

        Pattern pattern = Pattern.compile("[A-Z]{1,2}\\s?\\d{2}\\s?[A-Z]{3}");
        Matcher matcher = pattern.matcher(cleanedText);

        if (matcher.find()) {
            return matcher.group().replaceAll("\\s+", "");
        }

        return null;
    }

    private String detectWithOpenAI(File image) {
        try {
            String aiResult = openAIOcrService.detectPlateNumber(image);
            if (aiResult == null) {
                return null;
            }

            Pattern pattern = Pattern.compile("[A-Z]{1,2}\\s?\\d{2}\\s?[A-Z]{3}");
            Matcher matcher = pattern.matcher(aiResult.toUpperCase());
            if (matcher.find()) {
                return matcher.group().replaceAll("\\s+", "");
            }
        } catch (IOException e) {
            System.out.println("Eroare OpenAI: " + e.getMessage());
        }

        return null;
    }

    private LicensePlate savePlate(String plate, File imageFile) {
        List<LicensePlate> existing = plateRepository.findByPlateNumber(plate);
        LicensePlate lp;
        if (existing.isEmpty()) {
            lp = new LicensePlate(plate, imageFile.getAbsolutePath());
            plateRepository.save(lp);
        } else {
            lp = existing.get(0);
        }

        OcrHistory history = new OcrHistory(lp, imageFile.getName(), LocalDateTime.now());
        historyRepository.save(history);

        return lp;
    }
}
