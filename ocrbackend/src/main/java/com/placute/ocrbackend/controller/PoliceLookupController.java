package com.placute.ocrbackend.controller;

import com.placute.ocrbackend.dto.PoliceLookupDto;
import com.placute.ocrbackend.service.AuditLogService;
import com.placute.ocrbackend.service.PoliceLookupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/police")
public class PoliceLookupController {

    @Autowired
    private PoliceLookupService policeLookupService;

    @Autowired
    private AuditLogService auditLogService;

    @PreAuthorize("hasRole('POLICE')")
    @GetMapping("/lookup/{plateNumber}")
    public ResponseEntity<PoliceLookupDto> lookupByPlate(
            @PathVariable String plateNumber,
            Authentication authentication
    ) {
        PoliceLookupDto response = policeLookupService.lookupByPlateNumber(plateNumber);
        auditLogService.log(authentication, "POLICE_LOOKUP", response.getPlateNumber(), "Police vehicle lookup");
        return ResponseEntity.ok(response);
    }
}
