package com.placute.ocrbackend.controller;

import com.placute.ocrbackend.dto.OcrHistoryDto;
import com.placute.ocrbackend.model.OcrHistory;
import com.placute.ocrbackend.repository.OcrHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/plates")
@CrossOrigin(origins = "http://localhost:3000")
public class OcrHistoryController {

    @Autowired
    private OcrHistoryRepository historyRepository;

    @PreAuthorize("hasRole('POLICE')")
    @GetMapping("/history")
    public ResponseEntity<List<OcrHistoryDto>> getAllHistory() {
        List<OcrHistory> all = historyRepository.findAllWithLicensePlateOrderByProcessedAtDesc();

        List<OcrHistoryDto> dtoList = all.stream()
                .map(h -> {
                    var lp = h.getLicensePlate();
                    OcrHistoryDto.BboxDto bboxDto = new OcrHistoryDto.BboxDto(
                            h.getBboxX(),
                            h.getBboxY(),
                            h.getBboxW(),
                            h.getBboxH()
                    );
                    return new OcrHistoryDto(
                            h.getId(),
                            lp.getPlateNumber(),
                            lp.getBrand(),
                            lp.getModel(),
                            lp.getOwner(),
                            lp.getImagePath(),
                            h.getProcessedAt(),
                            h.getConfidence(),
                            bboxDto,
                            h.getReviewStatus(),
                            h.getReviewedBy(),
                            h.getReviewedAt(),
                            h.getReviewReason()
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    @PreAuthorize("hasRole('POLICE')")
    @GetMapping("/history/search")
    public ResponseEntity<List<OcrHistoryDto>> searchHistory(@RequestParam("query") String query) {
        List<OcrHistory> partial = historyRepository
                .findByPlateNumberContainingWithLicensePlateOrderByProcessedAtDesc(query);

        List<OcrHistoryDto> dtoList = partial.stream()
                .map(h -> {
                    var lp = h.getLicensePlate();
                    OcrHistoryDto.BboxDto bboxDto = new OcrHistoryDto.BboxDto(
                            h.getBboxX(),
                            h.getBboxY(),
                            h.getBboxW(),
                            h.getBboxH()
                    );
                    return new OcrHistoryDto(
                            h.getId(),
                            lp.getPlateNumber(),
                            lp.getBrand(),
                            lp.getModel(),
                            lp.getOwner(),
                            lp.getImagePath(),
                            h.getProcessedAt(),
                            h.getConfidence(),
                            bboxDto,
                            h.getReviewStatus(),
                            h.getReviewedBy(),
                            h.getReviewedAt(),
                            h.getReviewReason()
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }
}
