package com.placute.ocrbackend.controller;

import com.placute.ocrbackend.model.Insurance;
import com.placute.ocrbackend.model.LicensePlate;
import com.placute.ocrbackend.repository.InsuranceRepository;
import com.placute.ocrbackend.repository.LicensePlateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/api/insurance")
public class InsuranceController {

    @Autowired
    private InsuranceRepository insuranceRepository;

    @Autowired
    private LicensePlateRepository licensePlateRepository;

    @PreAuthorize("hasAnyRole('INSURANCE', 'POLICE')")
    @GetMapping("/{plateNumber}")
    public List<Insurance> getInsuranceByPlate(@PathVariable String plateNumber) {
        return insuranceRepository.findByLicensePlate_PlateNumber(plateNumber.trim().toUpperCase(Locale.ROOT));
    }


    @PreAuthorize("hasRole('INSURANCE')")
    @PostMapping
    public ResponseEntity<?> saveInsurance(@RequestBody Insurance insurance) {
        if (insurance == null || insurance.getLicensePlate() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Datele politei sunt invalide.");
        }
        if (insurance.getCompany() == null || insurance.getCompany().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Compania este obligatorie.");
        }
        if (insurance.getValidFrom() == null || insurance.getValidTo() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Intervalul de valabilitate este obligatoriu.");
        }
        if (insurance.getValidTo().isBefore(insurance.getValidFrom())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Data de expirare nu poate fi inainte de data de start.");
        }

        LicensePlate inputPlate = insurance.getLicensePlate();
        LicensePlate resolvedPlate = null;

        if (inputPlate.getId() != null) {
            Optional<LicensePlate> byId = licensePlateRepository.findById(inputPlate.getId());
            if (byId.isPresent()) {
                resolvedPlate = byId.get();
            }
        }

        if (resolvedPlate == null && inputPlate.getPlateNumber() != null && !inputPlate.getPlateNumber().isBlank()) {
            String normalizedPlate = inputPlate.getPlateNumber().trim().toUpperCase(Locale.ROOT);
            List<LicensePlate> matches = licensePlateRepository.findByPlateNumber(normalizedPlate);
            if (!matches.isEmpty()) {
                resolvedPlate = matches.get(0);
            }
        }

        if (resolvedPlate == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Placuta de inmatriculare nu exista.");
        }

        String plateNumber = resolvedPlate.getPlateNumber();
        List<Insurance> existing = insuranceRepository.findByLicensePlate_PlateNumber(plateNumber);

        boolean alreadyExists = existing.stream().anyMatch(ins ->
                ins.getCompany().equalsIgnoreCase(insurance.getCompany()) &&
                        ins.getValidFrom().equals(insurance.getValidFrom()) &&
                        ins.getValidTo().equals(insurance.getValidTo())
        );

        if (alreadyExists) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Aceasta asigurare exista deja pentru placuta.");
        }

        insurance.setCompany(insurance.getCompany().trim());
        insurance.setLicensePlate(resolvedPlate);
        Insurance saved = insuranceRepository.save(insurance);
        return ResponseEntity.ok(saved);
    }


}
