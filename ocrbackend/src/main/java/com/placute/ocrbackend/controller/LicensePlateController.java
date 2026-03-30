package com.placute.ocrbackend.controller;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.placute.ocrbackend.model.Insurance;
import com.placute.ocrbackend.model.LicensePlate;
import com.placute.ocrbackend.model.ParkingHistory;
import com.placute.ocrbackend.repository.InsuranceRepository;
import com.placute.ocrbackend.repository.LicensePlateRepository;
import com.placute.ocrbackend.repository.ParkingHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
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

    @PreAuthorize("hasRole('POLICE')")
    @GetMapping
    public List<LicensePlate> getAllPlates() {
        return licensePlateRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('POLICE', 'INSURANCE')")
    @GetMapping("/{plateNumber}")
    public List<LicensePlate> getByPlateNumber(@PathVariable String plateNumber) {
        return licensePlateRepository.findByPlateNumber(plateNumber);
    }

    @PreAuthorize("hasAnyRole('POLICE', 'PARKING')")
    @PostMapping
    public LicensePlate savePlate(@RequestBody LicensePlate plate) {
        if (plate.getDetectedAt() == null) {
            plate.setDetectedAt(LocalDateTime.now());
        }
        return licensePlateRepository.save(plate);
    }

    @PreAuthorize("hasAnyRole('POLICE', 'PARKING')")
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
        List<LicensePlate> plates = licensePlateRepository.findByPlateNumber(plateNumber);
        if (plates.isEmpty()) {
            throw new RuntimeException("Placuta nu exista");
        }
        LicensePlate plate = plates.get(plates.size() - 1);
        List<Insurance> insurances = insuranceRepository.findByLicensePlate_PlateNumber(plateNumber);
        List<ParkingHistory> parking = parkingHistoryRepository.findByLicensePlate_PlateNumber(plateNumber);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, BaseColor.BLACK);
            Font labelFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font textFont = new Font(Font.FontFamily.HELVETICA, 12);
            Font sectionTitleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, new BaseColor(68, 68, 68)); // gri închis
            Font redFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, new BaseColor(139, 0, 0)); // roșu sobru
            Font grayFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, new BaseColor(102, 102, 102)); // gri footer

            Paragraph title = new Paragraph("Fisa completa pentru placuta: " + plateNumber, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            if (plate.getImagePath() != null) {
                try {
                    Image img = Image.getInstance(plate.getImagePath());
                    img.scaleToFit(400, 200);
                    img.setAlignment(Image.ALIGN_CENTER);
                    document.add(img);

                    Paragraph caption = new Paragraph("Imaginea vehiculului", grayFont);
                    caption.setAlignment(Element.ALIGN_CENTER);
                    caption.setSpacingAfter(15);
                    document.add(caption);
                } catch (Exception e) {
                    document.add(new Paragraph("Imagine indisponibila", redFont));
                }
            }

            PdfPTable vehicleTable = new PdfPTable(2);
            vehicleTable.setWidthPercentage(100);
            vehicleTable.setSpacingBefore(10);
            vehicleTable.setSpacingAfter(15);
            vehicleTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            vehicleTable.addCell(new Phrase("Marca:", labelFont));
            vehicleTable.addCell(new Phrase(plate.getBrand() != null ? plate.getBrand() : "-", textFont));

            vehicleTable.addCell(new Phrase("Model:", labelFont));
            vehicleTable.addCell(new Phrase(plate.getModel() != null ? plate.getModel() : "-", textFont));

            vehicleTable.addCell(new Phrase("Proprietar:", labelFont));
            vehicleTable.addCell(new Phrase(plate.getOwner() != null ? plate.getOwner() : "-", textFont));

            vehicleTable.addCell(new Phrase("Detectat la:", labelFont));
            vehicleTable.addCell(new Phrase(plate.getDetectedAt().toString(), textFont));

            document.add(vehicleTable);

            Paragraph insuranceTitle = new Paragraph("Asigurari", sectionTitleFont);
            insuranceTitle.setSpacingAfter(10);
            document.add(insuranceTitle);

            if (insurances.isEmpty()) {
                document.add(new Paragraph("Nicio asigurare gasita.", redFont));
            } else {
                PdfPTable insuranceTable = new PdfPTable(3);
                insuranceTable.setWidthPercentage(100);
                insuranceTable.setSpacingBefore(5);
                insuranceTable.setSpacingAfter(15);

                insuranceTable.addCell("Companie");
                insuranceTable.addCell("Valabil de la");
                insuranceTable.addCell("Valabil pana");

                for (Insurance ins : insurances) {
                    insuranceTable.addCell(ins.getCompany());
                    insuranceTable.addCell(ins.getValidFrom().toString());
                    insuranceTable.addCell(ins.getValidTo().toString());
                }

                document.add(insuranceTable);
            }

            Paragraph parkingTitle = new Paragraph("Istoric parcare", sectionTitleFont);
            parkingTitle.setSpacingBefore(10);
            parkingTitle.setSpacingAfter(10);
            document.add(parkingTitle);

            if (parking.isEmpty()) {
                document.add(new Paragraph("Niciun istoric de parcare.", redFont));
            } else {
                PdfPTable parkingTable = new PdfPTable(2);
                parkingTable.setWidthPercentage(100);
                parkingTable.setSpacingBefore(5);
                parkingTable.setSpacingAfter(15);

                parkingTable.addCell("Intrare");
                parkingTable.addCell("Iesire");

                for (var p : parking) {
                    parkingTable.addCell(p.getEntryTime().toString());
                    parkingTable.addCell(p.getExitTime() != null ? p.getExitTime().toString() : "N/A");
                }

                document.add(parkingTable);
            }

            // Footer
            Paragraph footer = new Paragraph("Generat de ALPRo -" + LocalDateTime.now(), grayFont);
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
                .header("Content-Disposition", "attachment; filename=plate_" + plateNumber + ".pdf")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_PDF))
                .body(pdfBytes);
    }


}
