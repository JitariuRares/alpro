package com.placute.ocrbackend.controller;

import com.placute.ocrbackend.dto.ParkingSessionDto;
import com.placute.ocrbackend.service.ParkingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/parking")
public class ParkingHistoryController {

    @Autowired
    private ParkingService parkingService;

    @PreAuthorize("hasRole('PARKING')")
    @PostMapping(value = "/entry", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerEntry(
            @RequestParam("plateNumber") String plateNumber,
            @RequestParam("parkingZone") String parkingZone,
            @RequestParam(value = "entryTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime entryTime,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        try {
            ParkingSessionDto created = parkingService.registerEntry(plateNumber, parkingZone, entryTime, image);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Nu s-a putut salva dovada foto.");
        }
    }

    @PreAuthorize("hasRole('PARKING')")
    @PostMapping(value = "/exit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerExit(
            @RequestParam("plateNumber") String plateNumber,
            @RequestParam(value = "exitTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime exitTime,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        try {
            ParkingSessionDto updated = parkingService.registerExit(plateNumber, exitTime, image);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Nu s-a putut salva dovada foto.");
        }
    }

    @PreAuthorize("hasAnyRole('PARKING', 'POLICE')")
    @GetMapping("/{plateNumber}")
    public ResponseEntity<List<ParkingSessionDto>> getParkingSessionsByPlate(@PathVariable String plateNumber) {
        return ResponseEntity.ok(parkingService.getSessionsByPlate(plateNumber));
    }

    @PreAuthorize("hasAnyRole('PARKING', 'POLICE')")
    @GetMapping("/sessions/{sessionId}/evidence/{type}")
    public ResponseEntity<Resource> getParkingEvidence(
            @PathVariable Long sessionId,
            @PathVariable String type
    ) throws IOException {
        ParkingService.ParkingEvidencePayload payload = parkingService.getEvidence(sessionId, type);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .contentLength(payload.contentLength())
                .body(payload.resource());
    }
}
