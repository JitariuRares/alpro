package com.placute.ocrbackend.service;

import com.placute.ocrbackend.dto.DashboardStatsDto;
import com.placute.ocrbackend.model.LicensePlate;
import com.placute.ocrbackend.repository.InsuranceRepository;
import com.placute.ocrbackend.repository.LicensePlateRepository;
import com.placute.ocrbackend.repository.ParkingHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private LicensePlateRepository licensePlateRepository;

    @Autowired
    private InsuranceRepository insuranceRepository;

    @Autowired
    private ParkingHistoryRepository parkingHistoryRepository;

    public DashboardStatsDto getDashboardStats() {
        long totalPlates = licensePlateRepository.count();
        long totalInsurances = insuranceRepository.count();
        long totalParkings = parkingHistoryRepository.count();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        List<LicensePlate> recentPlates = licensePlateRepository.findByDetectedAtAfter(sevenDaysAgo);

        Map<String, Long> countyCounts = recentPlates.stream()
                .map(lp -> extractCountyPrefix(lp.getPlateNumber()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        Map<String, Long> topCounties = countyCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        return new DashboardStatsDto(totalPlates, totalInsurances, totalParkings, topCounties);
    }

    private String extractCountyPrefix(String plate) {
        if (plate == null || plate.length() < 2) return null;
        if (plate.matches("^[A-Z]{2}.*")) {
            return plate.substring(0, 2);
        } else if (plate.matches("^[A-Z]{1}.*")) {
            return plate.substring(0, 1);
        }
        return null;
    }
}
