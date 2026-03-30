package com.placute.ocrbackend.controller;

import com.placute.ocrbackend.dto.OcrPlateDto;
import com.placute.ocrbackend.model.LicensePlate;
import com.placute.ocrbackend.repository.LicensePlateRepository;
import com.placute.ocrbackend.service.OcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class OcrController {

    @Autowired
    private OcrService ocrService;

    @Autowired
    private LicensePlateRepository plateRepository;

    @PreAuthorize("hasAnyRole('POLICE', 'PARKING')")
    @PostMapping("/ocr")
    public ResponseEntity<String> uploadImage(@RequestParam("image") MultipartFile file) throws IOException {
        File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename());
        file.transferTo(convFile);

        String result = ocrService.recognizeText(convFile);
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAnyRole('POLICE', 'PARKING')")
    @PostMapping("/ocr/full")
    public ResponseEntity<?> uploadImageFull(@RequestParam("image") MultipartFile file) throws IOException {
        File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename());
        file.transferTo(convFile);

        LicensePlate lp = ocrService.recognizeAndReturnPlate(convFile);
        if (lp == null) {
            return ResponseEntity.status(404).body("Nicio plăcuta valida gasita.");
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
                lp.getUser() != null ? lp.getUser().getRole().name() : null
        );
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasAnyRole('POLICE', 'PARKING')")
    @GetMapping("/plates")
    public ResponseEntity<List<LicensePlate>> getAllPlates() {
        List<LicensePlate> plates = plateRepository.findAll();
        return ResponseEntity.ok(plates);
    }
}
