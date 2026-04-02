package com.placute.ocrbackend.controller;

import com.placute.ocrbackend.model.LicensePlate;
import com.placute.ocrbackend.model.ParkingHistory;
import com.placute.ocrbackend.repository.LicensePlateRepository;
import com.placute.ocrbackend.repository.ParkingHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/parking")
public class ParkingHistoryController {

    @Autowired
    private ParkingHistoryRepository parkingHistoryRepository;

    @Autowired
    private LicensePlateRepository licensePlateRepository;

    @PreAuthorize("hasAnyRole('PARKING', 'POLICE')")
    @PostMapping
    public ResponseEntity<?> addParkingRecord(@RequestBody ParkingHistory record) {
        if (record == null || record.getLicensePlate() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Datele parcarii sunt invalide.");
        }
        if (record.getEntryTime() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Data de intrare este obligatorie.");
        }
        if (record.getExitTime() != null && record.getExitTime().isBefore(record.getEntryTime())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Data de iesire nu poate fi inainte de intrare.");
        }

        String rawPlate = record.getLicensePlate().getPlateNumber();
        String plateNumber = rawPlate == null ? "" : rawPlate.trim().toUpperCase(Locale.ROOT);
        if (plateNumber.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Numarul placutei este obligatoriu.");
        }

        List<LicensePlate> plates = licensePlateRepository.findByPlateNumber(plateNumber);
        if (plates.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Placuta de inmatriculare nu exista in baza de date.");
        }

        List<ParkingHistory> existing = parkingHistoryRepository.findByLicensePlate_PlateNumber(plateNumber);
        boolean duplicate = existing.stream().anyMatch(e ->
                e.getEntryTime().equals(record.getEntryTime()) &&
                        ((e.getExitTime() == null && record.getExitTime() == null) ||
                                (e.getExitTime() != null && e.getExitTime().equals(record.getExitTime())))
        );

        if (duplicate) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Aceasta inregistrare de parcare exista deja.");
        }

        record.setLicensePlate(plates.get(0));
        ParkingHistory saved = parkingHistoryRepository.save(record);
        return ResponseEntity.ok(saved);
    }



    @PreAuthorize("hasAnyRole('PARKING', 'POLICE')")
    @GetMapping("/{plateNumber}")
    public List<ParkingHistory> getParkingByPlate(@PathVariable String plateNumber) {
        return parkingHistoryRepository.findByLicensePlate_PlateNumber(plateNumber);
    }

}
