package com.placute.ocrbackend.controller;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.placute.ocrbackend.model.Insurance;
import com.placute.ocrbackend.model.LicensePlate;
import com.placute.ocrbackend.model.OcrHistory;
import com.placute.ocrbackend.model.ParkingHistory;
import com.placute.ocrbackend.model.VideoDetection;
import com.placute.ocrbackend.repository.AuditLogRepository;
import com.placute.ocrbackend.repository.InsuranceRepository;
import com.placute.ocrbackend.repository.LicensePlateRepository;
import com.placute.ocrbackend.repository.OcrHistoryRepository;
import com.placute.ocrbackend.repository.ParkingHistoryRepository;
import com.placute.ocrbackend.repository.VideoDetectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/license-plates")
@CrossOrigin(origins = "http://localhost:3000")
public class LicensePlateController {

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
    private AuditLogRepository auditLogRepository;

    @PreAuthorize("hasRole('POLICE')")
    @GetMapping
    public List<LicensePlate> getAllPlates() {
        return licensePlateRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('POLICE', 'INSURANCE')")
    @GetMapping("/{plateNumber}")
    public List<LicensePlate> getByPlateNumber(@PathVariable String plateNumber) {
        return licensePlateRepository.findByPlateNumber(plateNumber.trim().toUpperCase(Locale.ROOT));
    }

    @PreAuthorize("hasRole('POLICE')")
    @PostMapping
    public LicensePlate savePlate(@RequestBody LicensePlate plate) {
        if (plate == null || plate.getPlateNumber() == null || plate.getPlateNumber().isBlank()) {
            throw new RuntimeException("Numarul placutei este obligatoriu.");
        }
        String normalizedPlate = plate.getPlateNumber().trim().toUpperCase(Locale.ROOT);
        plate.setPlateNumber(normalizedPlate);

        if (!licensePlateRepository.findByPlateNumber(normalizedPlate).isEmpty()) {
            throw new RuntimeException("Placuta exista deja in baza de date.");
        }
        if (plate.getDetectedAt() == null) {
            plate.setDetectedAt(LocalDateTime.now());
        }
        return licensePlateRepository.save(plate);
    }

    @PreAuthorize("hasRole('POLICE')")
    @PutMapping("/{id}")
    public ResponseEntity<LicensePlate> updatePlateDetails(
            @PathVariable Long id,
            @RequestBody LicensePlate updatedData) {

        Optional<LicensePlate> optionalPlate = licensePlateRepository.findById(Objects.requireNonNull(id, "id is required"));
        if (!optionalPlate.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        LicensePlate existing = optionalPlate.get();
        existing.setBrand(updatedData.getBrand());
        existing.setModel(updatedData.getModel());
        existing.setOwner(updatedData.getOwner());

        LicensePlate saved = licensePlateRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    @PreAuthorize("hasRole('POLICE')")
    @GetMapping("/pdf/{plateNumber}")
    public ResponseEntity<byte[]> exportPdf(@PathVariable String plateNumber) throws IOException {
        String normalizedPlate = plateNumber.trim().toUpperCase(Locale.ROOT);
        List<LicensePlate> plates = licensePlateRepository.findByPlateNumber(normalizedPlate);
        if (plates.isEmpty()) {
            throw new RuntimeException("Placuta nu exista");
        }
        LicensePlate plate = plates.get(plates.size() - 1);
        List<Insurance> insurances = insuranceRepository.findByLicensePlate_PlateNumberOrderByValidToDesc(normalizedPlate);
        List<ParkingHistory> parking = parkingHistoryRepository.findByLicensePlate_PlateNumberOrderByEntryTimeDesc(normalizedPlate);
        List<OcrHistory> ocrDetections = ocrHistoryRepository
                .findByExactPlateNumberWithLicensePlateOrderByProcessedAtDesc(normalizedPlate)
                .stream()
                .limit(15)
                .toList();
        List<VideoDetection> videoDetections = videoDetectionRepository.findTop100ByPlateTextOrderByIdDesc(normalizedPlate)
                .stream()
                .limit(20)
                .toList();
        var auditEvents = auditLogRepository.findTop10ByTargetPlateNumberOrderByCreatedAtDescIdDesc(normalizedPlate);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            BaseColor brandBlue = new BaseColor(37, 99, 235);
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, new BaseColor(17, 24, 39));
            Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, new BaseColor(71, 85, 105));
            Font labelFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font textFont = new Font(Font.FontFamily.HELVETICA, 12);
            Font sectionTitleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, brandBlue);
            Font tableHeaderFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(30, 41, 59));
            Font tableFont = new Font(Font.FontFamily.HELVETICA, 9);
            Font emptyFont = new Font(Font.FontFamily.HELVETICA, 11, Font.ITALIC, new BaseColor(100, 116, 139));
            Font grayFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, new BaseColor(100, 116, 139));

            Paragraph title = new Paragraph("ALPRo Vehicle Case", titleFont);
            title.setAlignment(Element.ALIGN_LEFT);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph subtitle = new Paragraph(
                    "Raport operational pentru placuta " + normalizedPlate
                            + " | generat la " + formatDateTime(LocalDateTime.now()),
                    subtitleFont
            );
            subtitle.setSpacingAfter(14);
            document.add(subtitle);

            PdfPTable metadata = infoTable();
            addInfoRow(metadata, "Sistem", "ALPRo operational control", labelFont, textFont);
            addInfoRow(metadata, "Nivel acces", "POLICE", labelFont, textFont);
            addInfoRow(metadata, "Scop raport", "Vehicle Case", labelFont, textFont);
            document.add(metadata);

            if (plate.getImagePath() != null) {
                try {
                    Image img = loadImageForPdf(plate.getImagePath());
                    img.scaleToFit(430, 220);
                    img.setAlignment(Image.ALIGN_CENTER);
                    document.add(img);

                    Paragraph caption = new Paragraph("Imagine asociata ultimei inregistrari vehicul", grayFont);
                    caption.setAlignment(Element.ALIGN_CENTER);
                    caption.setSpacingAfter(15);
                    document.add(caption);
                } catch (Exception e) {
                    document.add(new Paragraph("Imagine indisponibila pentru raport.", emptyFont));
                }
            }

            addSectionTitle(document, "Date vehicul", sectionTitleFont);

            PdfPTable vehicleTable = infoTable();
            addInfoRow(vehicleTable, "Placuta", normalizedPlate, labelFont, textFont);
            addInfoRow(vehicleTable, "Marca", safe(plate.getBrand()), labelFont, textFont);
            addInfoRow(vehicleTable, "Model", safe(plate.getModel()), labelFont, textFont);
            addInfoRow(vehicleTable, "Proprietar", safe(plate.getOwner()), labelFont, textFont);
            addInfoRow(vehicleTable, "Detectat la", formatDateTime(plate.getDetectedAt()), labelFont, textFont);
            addInfoRow(vehicleTable, "Incredere detectie", formatConfidence(plate.getConfidence()), labelFont, textFont);
            addInfoRow(vehicleTable, "Bounding box",
                    bboxText(plate.getBboxX(), plate.getBboxY(), plate.getBboxW(), plate.getBboxH()),
                    labelFont,
                    textFont
            );
            document.add(vehicleTable);

            addSectionTitle(document, "Asigurari", sectionTitleFont);

            if (insurances.isEmpty()) {
                document.add(new Paragraph("Nu exista asigurari inregistrate pentru aceasta placuta.", emptyFont));
            } else {
                PdfPTable insuranceTable = new PdfPTable(new float[]{2.2f, 1.4f, 1.4f});
                insuranceTable.setWidthPercentage(100);
                insuranceTable.setSpacingBefore(5);
                insuranceTable.setSpacingAfter(15);

                addHeaderCells(insuranceTable, tableHeaderFont, "Companie", "Valabil de la", "Valabil pana");

                for (Insurance ins : insurances) {
                    addTextCell(insuranceTable, safe(ins.getCompany()), tableFont);
                    addTextCell(insuranceTable, formatDate(ins.getValidFrom()), tableFont);
                    addTextCell(insuranceTable, formatDate(ins.getValidTo()), tableFont);
                }

                document.add(insuranceTable);
            }

            addSectionTitle(document, "Istoric parcare", sectionTitleFont);

            if (parking.isEmpty()) {
                document.add(new Paragraph("Nu exista sesiuni de parcare pentru aceasta placuta.", emptyFont));
            } else {
                PdfPTable parkingTable = new PdfPTable(new float[]{1.8f, 1.8f, 1.2f});
                parkingTable.setWidthPercentage(100);
                parkingTable.setSpacingBefore(5);
                parkingTable.setSpacingAfter(15);

                addHeaderCells(parkingTable, tableHeaderFont, "Intrare", "Iesire", "Status");

                for (ParkingHistory p : parking) {
                    addTextCell(parkingTable, formatDateTime(p.getEntryTime()), tableFont);
                    addTextCell(parkingTable, formatDateTime(p.getExitTime()), tableFont);
                    addTextCell(parkingTable, p.getStatus() != null ? p.getStatus().name() : "-", tableFont);
                }

                document.add(parkingTable);
            }

            addSectionTitle(document, "Detectii imagine", sectionTitleFont);

            if (ocrDetections.isEmpty()) {
                document.add(new Paragraph("Nu exista detectii imagine pentru aceasta placuta.", emptyFont));
            } else {
                PdfPTable detectionTable = new PdfPTable(new float[]{1.5f, 1.2f, 1.2f, 1.6f});
                detectionTable.setWidthPercentage(100);
                detectionTable.setSpacingBefore(5);
                detectionTable.setSpacingAfter(15);

                addHeaderCells(detectionTable, tableHeaderFont, "Procesat la", "Incredere", "Review", "Fisier");

                for (OcrHistory history : ocrDetections) {
                    addTextCell(detectionTable, formatDateTime(history.getProcessedAt()), tableFont);
                    addTextCell(detectionTable, formatConfidence(history.getConfidence()), tableFont);
                    addTextCell(detectionTable,
                            reviewStatusLabel(
                                    history.getReviewStatus() != null ? history.getReviewStatus().name() : "DE_REVIEW",
                                    history.getReviewReason()
                            ),
                            tableFont
                    );
                    addTextCell(detectionTable, safe(history.getFilename()), tableFont);
                }

                document.add(detectionTable);
            }

            addSectionTitle(document, "Detectii video", sectionTitleFont);

            if (videoDetections.isEmpty()) {
                document.add(new Paragraph("Nu exista detectii video pentru aceasta placuta.", emptyFont));
            } else {
                PdfPTable videoTable = new PdfPTable(new float[]{1f, 1.1f, 1.2f, 1.2f, 1.4f});
                videoTable.setWidthPercentage(100);
                videoTable.setSpacingBefore(5);
                videoTable.setSpacingAfter(15);

                addHeaderCells(videoTable, tableHeaderFont, "Frame", "Timp video", "Incredere", "Review", "Track");

                for (VideoDetection detection : videoDetections) {
                    addTextCell(videoTable,
                            detection.getFrameIndex() != null ? detection.getFrameIndex().toString() : "-",
                            tableFont
                    );
                    addTextCell(videoTable, formatVideoTimestamp(detection.getTimestampMs()), tableFont);
                    addTextCell(videoTable, formatConfidence(detection.getConfidence()), tableFont);
                    addTextCell(videoTable,
                            reviewStatusLabel(
                                    detection.getReviewStatus() != null ? detection.getReviewStatus().name() : "DE_REVIEW",
                                    detection.getReviewReason()
                            ),
                            tableFont
                    );
                    addTextCell(videoTable,
                            detection.getTrackId() != null ? detection.getTrackId().toString() : "-",
                            tableFont
                    );
                }

                document.add(videoTable);
            }

            addSectionTitle(document, "Audit relevant", sectionTitleFont);

            if (auditEvents.isEmpty()) {
                document.add(new Paragraph("Nu exista evenimente de audit asociate acestei placute.", emptyFont));
            } else {
                PdfPTable auditTable = new PdfPTable(new float[]{1.5f, 1.2f, 1.4f, 2.4f});
                auditTable.setWidthPercentage(100);
                auditTable.setSpacingBefore(5);
                auditTable.setSpacingAfter(15);

                addHeaderCells(auditTable, tableHeaderFont, "Timp", "Actor", "Actiune", "Detalii");

                for (var event : auditEvents) {
                    addTextCell(auditTable, formatDateTime(event.getCreatedAt()), tableFont);
                    addTextCell(auditTable, safe(event.getActorUsername()), tableFont);
                    addTextCell(auditTable, safe(event.getAction()), tableFont);
                    addTextCell(auditTable, safe(event.getDetails()), tableFont);
                }

                document.add(auditTable);
            }

            Paragraph note = new Paragraph(
                    "Nota: raportul reflecta datele disponibile in sistem la momentul generarii.",
                    grayFont
            );
            note.setSpacingBefore(12);
            document.add(note);

            Paragraph footer = new Paragraph("Generat de ALPRo - " + formatDateTime(LocalDateTime.now()), grayFont);
            footer.setAlignment(Element.ALIGN_RIGHT);
            footer.setSpacingBefore(30);
            document.add(footer);

        } catch (DocumentException e) {
            throw new IOException("Eroare la generarea PDF-ului: " + e.getMessage());
        } finally {
            document.close();
        }

        byte[] pdfBytes = out.toByteArray();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=plate_" + normalizedPlate + ".pdf")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_PDF))
                .body(pdfBytes);
    }

    private void addSectionTitle(Document document, String title, Font font) throws DocumentException {
        Paragraph paragraph = new Paragraph(title, font);
        paragraph.setSpacingBefore(12);
        paragraph.setSpacingAfter(8);
        document.add(paragraph);
    }

    private PdfPTable infoTable() throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1.2f, 3.8f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(12);
        table.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        return table;
    }

    private void addInfoRow(PdfPTable table, String label, String value, Font labelFont, Font textFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label + ":", labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(6);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(safe(value), textFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(6);
        table.addCell(valueCell);
    }

    private void addHeaderCells(PdfPTable table, Font font, String... headers) {
        BaseColor background = new BaseColor(239, 246, 255);
        BaseColor border = new BaseColor(218, 226, 238);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(background);
            cell.setBorderColor(border);
            cell.setPadding(7);
            table.addCell(cell);
        }
    }

    private void addTextCell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(safe(value), font));
        cell.setBorderColor(new BaseColor(226, 232, 240));
        cell.setPadding(7);
        table.addCell(cell);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String formatDate(LocalDate value) {
        return value == null ? "-" : value.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    private String formatConfidence(Double confidence) {
        if (confidence == null) {
            return "-";
        }
        double percent = confidence <= 1 ? confidence * 100 : confidence;
        return String.format(Locale.ROOT, "%.1f%%", percent);
    }

    private String formatVideoTimestamp(Long timestampMs) {
        if (timestampMs == null) {
            return "-";
        }
        long totalSeconds = timestampMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    private String bboxText(Integer x, Integer y, Integer w, Integer h) {
        if (x == null || y == null || w == null || h == null) {
            return "-";
        }
        return "x=" + x + ", y=" + y + ", w=" + w + ", h=" + h;
    }

    private String reviewStatusLabel(String status, String reason) {
        if (reason == null || reason.isBlank()) {
            return status;
        }
        return status + " - " + reason;
    }

    private Image loadImageForPdf(String imagePath) throws IOException, BadElementException {
        try {
            return Image.getInstance(imagePath);
        } catch (Exception ignored) {
            BufferedImage buffered = ImageIO.read(new File(imagePath));
            if (buffered == null) {
                throw new IOException("Format imagine nesuportat pentru PDF");
            }

            ByteArrayOutputStream imageBuffer = new ByteArrayOutputStream();
            boolean writeOk = ImageIO.write(buffered, "png", imageBuffer);
            if (!writeOk) {
                throw new IOException("Nu s-a putut converti imaginea pentru PDF");
            }

            return Image.getInstance(imageBuffer.toByteArray());
        }
    }
}
