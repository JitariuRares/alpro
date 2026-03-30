package com.placute.ocrbackend.controller;

import com.placute.ocrbackend.model.Insurance;
import com.placute.ocrbackend.repository.InsuranceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/insurance")
public class InsuranceController {

    @Autowired
    private InsuranceRepository insuranceRepository;

    @PreAuthorize("hasAnyRole('INSURANCE', 'POLICE')")
    @GetMapping("/{plateNumber}")
    public List<Insurance> getInsuranceByPlate(@PathVariable String plateNumber) {
        return insuranceRepository.findByLicensePlate_PlateNumber(plateNumber);
    }


    @PreAuthorize("hasRole('INSURANCE')")
    @PostMapping
    public ResponseEntity<?> saveInsurance(@RequestBody Insurance insurance) {
        String plateNumber = insurance.getLicensePlate().getPlateNumber();
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

        Insurance saved = insuranceRepository.save(insurance);
        return ResponseEntity.ok(saved);
    }


}
