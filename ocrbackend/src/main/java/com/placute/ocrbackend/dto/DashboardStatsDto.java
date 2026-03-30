package com.placute.ocrbackend.dto;

import java.util.Map;

public class DashboardStatsDto {
    private long totalPlates;
    private long totalInsurances;
    private long totalParkings;
    private Map<String, Long> topCountiesLast7Days;

    public DashboardStatsDto(long totalPlates, long totalInsurances, long totalParkings, Map<String, Long> topCountiesLast7Days) {
        this.totalPlates = totalPlates;
        this.totalInsurances = totalInsurances;
        this.totalParkings = totalParkings;
        this.topCountiesLast7Days = topCountiesLast7Days;
    }

    public long getTotalPlates() {
        return totalPlates;
    }

    public long getTotalInsurances() {
        return totalInsurances;
    }

    public long getTotalParkings() {
        return totalParkings;
    }

    public Map<String, Long> getTopCountiesLast7Days() {
        return topCountiesLast7Days;
    }
}
