package ru.spb.transit.web.dto;

import java.math.BigDecimal;

public record ReliabilityResponse(
        Long tripId,
        int periodDays,
        BigDecimal averageDelayMinutes,
        BigDecimal delayedTripsPercent,
        BigDecimal cancelledTripsPercent,
        long tripsCount,
        String riskLevel,
        boolean dataSufficient
) {
}
