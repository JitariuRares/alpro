package com.placute.ocrbackend.controller;

import com.placute.ocrbackend.model.Insurance;
import com.placute.ocrbackend.model.LicensePlate;
import com.placute.ocrbackend.repository.InsuranceRepository;
import com.placute.ocrbackend.repository.LicensePlateRepository;
import com.placute.ocrbackend.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    @Autowired
    private AuditLogService auditLogService;

    @PreAuthorize("hasAnyRole('INSURANCE', 'POLICE')")
    @GetMapping("/{plateNumber}")
    public List<Insurance> getInsuranceByPlate(@PathVariable String plateNumber) {
        return insuranceRepository.findByLicensePlate_PlateNumber(plateNumber.trim().toUpperCase(Locale.ROOT));
    }


    @PreAuthorize("hasRole('INSURANCE')")
    @PostMapping
    public ResponseEntity<?> saveInsurance(@RequestBody Insurance insurance, Authentication authentication) {
        if (insurance == null || insurance.getLicensePlate() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Datele politei sunt invalide.");
        }
        if (insurance.getCompany() == null || insurance.getCompany().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Compania este obligatorie.");
        }
        if (insurance.getPolicyNumber() == null || insurance.getPolicyNumber().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Numarul politei este obligatoriu.");
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
                safeEquals(ins.getPolicyNumber(), insurance.getPolicyNumber()) &&
                        ins.getCompany().equalsIgnoreCase(insurance.getCompany()) &&
                        ins.getValidFrom().equals(insurance.getValidFrom()) &&
                        ins.getValidTo().equals(insurance.getValidTo())
        );

        if (alreadyExists) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Aceasta asigurare exista deja pentru placuta.");
        }

        insurance.setCompany(insurance.getCompany().trim());
        insurance.setPolicyNumber(insurance.getPolicyNumber().trim().toUpperCase(Locale.ROOT));
        insurance.setPolicyType(normalizePolicyType(insurance.getPolicyType()));
        insurance.setNotes(normalizeNotes(insurance.getNotes()));
        insurance.setLicensePlate(resolvedPlate);
        Insurance saved = insuranceRepository.save(insurance);
        auditLogService.log(
                authentication,
                "INSURANCE_CREATE",
                plateNumber,
                "Polita creata, id=" + saved.getId()
        );
        return ResponseEntity.ok(saved);
    }

    @PreAuthorize("hasRole('INSURANCE')")
    @PutMapping("/{insuranceId}")
    public ResponseEntity<?> updateInsurance(
            @PathVariable Long insuranceId,
            @RequestBody Insurance insurance,
            Authentication authentication
    ) {
        if (insurance == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Datele politei sunt invalide.");
        }
        if (insurance.getCompany() == null || insurance.getCompany().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Compania este obligatorie.");
        }
        if (insurance.getPolicyNumber() == null || insurance.getPolicyNumber().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Numarul politei este obligatoriu.");
        }
        if (insurance.getValidFrom() == null || insurance.getValidTo() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Intervalul de valabilitate este obligatoriu.");
        }
        if (insurance.getValidTo().isBefore(insurance.getValidFrom())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Data de expirare nu poate fi inainte de data de start.");
        }

        Insurance existing = insuranceRepository.findById(insuranceId)
                .orElse(null);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Polita nu exista.");
        }

        existing.setCompany(insurance.getCompany().trim());
        existing.setPolicyNumber(insurance.getPolicyNumber().trim().toUpperCase(Locale.ROOT));
        existing.setPolicyType(normalizePolicyType(insurance.getPolicyType()));
        existing.setValidFrom(insurance.getValidFrom());
        existing.setValidTo(insurance.getValidTo());
        existing.setNotes(normalizeNotes(insurance.getNotes()));

        Insurance saved = insuranceRepository.save(existing);
        String plateNumber = saved.getLicensePlate() != null ? saved.getLicensePlate().getPlateNumber() : null;
        auditLogService.log(
                authentication,
                "INSURANCE_UPDATE",
                plateNumber,
                "Polita actualizata, id=" + saved.getId()
        );
        return ResponseEntity.ok(saved);
    }

    private boolean safeEquals(String first, String second) {
        if (first == null || second == null) {
            return false;
        }
        return first.trim().equalsIgnoreCase(second.trim());
    }

    private String normalizePolicyType(String policyType) {
        if (policyType == null || policyType.isBlank()) {
            return "RCA";
        }
        String normalized = policyType.trim().toUpperCase(Locale.ROOT);
        if (!List.of("RCA", "CASCO", "ALT TIP").contains(normalized)) {
            return "ALT TIP";
        }
        return normalized;
    }

    private String normalizeNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        String normalized = notes.trim();
        return normalized.length() > 500 ? normalized.substring(0, 500) : normalized;
    }

}
