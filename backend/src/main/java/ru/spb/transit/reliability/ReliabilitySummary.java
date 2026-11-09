package ru.spb.transit.reliability;

import java.math.BigDecimal;

/**
 * Результат расчёта надёжности по историческим агрегатам.
 * Проценты возвращаются в диапазоне 0..100 с точностью до двух знаков.
 */
public record ReliabilitySummary(
        int periodDays,
        long tripsCount,
        long delayedTrips,
        long cancelledTrips,
        BigDecimal averageDelayMinutes,
        BigDecimal delayedTripsPercent,
        BigDecimal cancellationPercent,
        RiskLevel riskLevel,
        boolean dataSufficient
) {
}
