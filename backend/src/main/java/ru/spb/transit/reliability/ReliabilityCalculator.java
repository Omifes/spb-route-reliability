package ru.spb.transit.reliability;

import org.springframework.stereotype.Component;
import ru.spb.transit.domain.HistoricalStatistic;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Считает показатели надёжности по суточным агрегатам.
 *
 * <p>Средняя задержка взвешивается по числу поездок, поэтому дни с большим
 * количеством поездок влияют сильнее. Пороговые значения вынесены в константы
 * и могут быть уточнены после анализа реальных данных.</p>
 */
@Component
public class ReliabilityCalculator {

    public static final int MIN_TRIPS_FOR_ESTIMATE = 10;

    public static final BigDecimal MEDIUM_AVERAGE_DELAY = BigDecimal.valueOf(4);
    public static final BigDecimal HIGH_AVERAGE_DELAY = BigDecimal.valueOf(8);
    public static final BigDecimal MEDIUM_DELAYED_PERCENT = BigDecimal.valueOf(12);
    public static final BigDecimal HIGH_DELAYED_PERCENT = BigDecimal.valueOf(25);
    public static final BigDecimal HIGH_CANCELLATION_PERCENT = BigDecimal.valueOf(5);

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public ReliabilitySummary calculate(List<HistoricalStatistic> statistics, int periodDays) {
        if (periodDays <= 0) {
            throw new IllegalArgumentException("periodDays must be positive");
        }

        long trips = 0;
        long delayed = 0;
        long cancelled = 0;
        long delayWeight = 0;
        BigDecimal weightedDelay = BigDecimal.ZERO;

        for (HistoricalStatistic statistic : statistics) {
            trips += statistic.getTripsCount();
            delayed += statistic.getDelayedTrips();
            cancelled += statistic.getCancelledTrips();

            if (statistic.getAverageDelay() != null && statistic.getTripsCount() > 0) {
                weightedDelay = weightedDelay.add(
                        statistic.getAverageDelay().multiply(BigDecimal.valueOf(statistic.getTripsCount())));
                delayWeight += statistic.getTripsCount();
            }
        }

        BigDecimal averageDelay = delayWeight == 0
                ? null
                : weightedDelay.divide(BigDecimal.valueOf(delayWeight), 2, RoundingMode.HALF_UP);

        BigDecimal delayedPercent = percent(delayed, trips);
        BigDecimal cancelledPercent = percent(cancelled, trips);

        boolean dataSufficient = trips >= MIN_TRIPS_FOR_ESTIMATE && averageDelay != null;
        RiskLevel riskLevel = dataSufficient
                ? classify(averageDelay, delayedPercent, cancelledPercent)
                : RiskLevel.UNKNOWN;

        return new ReliabilitySummary(
                periodDays,
                trips,
                delayed,
                cancelled,
                averageDelay,
                delayedPercent,
                cancelledPercent,
                riskLevel,
                dataSufficient);
    }

    private RiskLevel classify(BigDecimal averageDelay, BigDecimal delayedPercent, BigDecimal cancelledPercent) {
        if (averageDelay.compareTo(HIGH_AVERAGE_DELAY) >= 0
                || delayedPercent.compareTo(HIGH_DELAYED_PERCENT) >= 0
                || cancelledPercent.compareTo(HIGH_CANCELLATION_PERCENT) >= 0) {
            return RiskLevel.HIGH;
        }
        if (averageDelay.compareTo(MEDIUM_AVERAGE_DELAY) >= 0
                || delayedPercent.compareTo(MEDIUM_DELAYED_PERCENT) >= 0) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private BigDecimal percent(long part, long total) {
        if (total == 0) {
            return null;
        }
        return BigDecimal.valueOf(part)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
