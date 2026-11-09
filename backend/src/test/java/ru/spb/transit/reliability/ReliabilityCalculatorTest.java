package ru.spb.transit.reliability;

import org.junit.jupiter.api.Test;
import ru.spb.transit.domain.HistoricalStatistic;
import ru.spb.transit.domain.Route;
import ru.spb.transit.domain.RouteType;
import ru.spb.transit.domain.Stop;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReliabilityCalculatorTest {

    private final ReliabilityCalculator calculator = new ReliabilityCalculator();
    private final Route route = new Route("route-1", "Маршрут 1", RouteType.BUS);
    private final Stop stop = new Stop("stop-1", "Остановка", BigDecimal.ZERO, BigDecimal.ZERO);

    @Test
    void returnsUnknownWhenThereIsNotEnoughData() {
        ReliabilitySummary result = calculator.calculate(List.of(stat(1, 5, 0, 0, "10.00")), 30);

        assertThat(result.dataSufficient()).isFalse();
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.UNKNOWN);
        assertThat(result.averageDelayMinutes()).isEqualByComparingTo("10.00");
        assertThat(result.tripsCount()).isEqualTo(5);
    }

    @Test
    void returnsUnknownWhenAverageDelayIsMissing() {
        ReliabilitySummary result = calculator.calculate(
                List.of(stat(1, 100, 10, 0, null)), 30);

        assertThat(result.dataSufficient()).isFalse();
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.UNKNOWN);
        assertThat(result.averageDelayMinutes()).isNull();
    }

    @Test
    void weightsAverageDelayByTripsCount() {
        ReliabilitySummary result = calculator.calculate(List.of(
                stat(1, 10, 0, 0, "10.00"),
                stat(2, 30, 0, 0, "2.00")
        ), 30);

        assertThat(result.tripsCount()).isEqualTo(40);
        assertThat(result.averageDelayMinutes()).isEqualByComparingTo("4.00");
        assertThat(result.delayedTripsPercent()).isEqualByComparingTo("0.00");
    }

    @Test
    void classifiesLowRisk() {
        ReliabilitySummary result = calculator.calculate(
                List.of(stat(1, 100, 5, 0, "1.50")), 30);

        assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(result.delayedTripsPercent()).isEqualByComparingTo("5.00");
    }

    @Test
    void classifiesMediumRiskByDelayedPercent() {
        ReliabilitySummary result = calculator.calculate(
                List.of(stat(1, 100, 15, 0, "3.00")), 30);

        assertThat(result.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(result.delayedTripsPercent()).isEqualByComparingTo("15.00");
    }

    @Test
    void classifiesHighRiskByDelayedPercent() {
        ReliabilitySummary result = calculator.calculate(
                List.of(stat(1, 100, 30, 0, "3.00")), 30);

        assertThat(result.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.delayedTripsPercent()).isEqualByComparingTo("30.00");
    }

    @Test
    void classifiesHighRiskByCancellationPercent() {
        ReliabilitySummary result = calculator.calculate(
                List.of(stat(1, 100, 1, 6, "1.00")), 30);

        assertThat(result.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.cancellationPercent()).isEqualByComparingTo("6.00");
    }

    @Test
    void classifiesHighRiskByAverageDelay() {
        ReliabilitySummary result = calculator.calculate(
                List.of(stat(1, 100, 1, 0, "8.00")), 30);

        assertThat(result.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.averageDelayMinutes()).isEqualByComparingTo("8.00");
    }

    @Test
    void roundsPercentagesToTwoDigits() {
        ReliabilitySummary result = calculator.calculate(
                List.of(stat(1, 3, 1, 0, "1.00")), 30);

        assertThat(result.delayedTripsPercent()).isEqualByComparingTo("33.33");
    }

    @Test
    void rejectsNonPositivePeriod() {
        assertThatThrownBy(() -> calculator.calculate(List.of(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private HistoricalStatistic stat(int dayOffset, int trips, int delayed, int cancelled, String averageDelay) {
        BigDecimal delay = averageDelay == null ? null : new BigDecimal(averageDelay);
        return new HistoricalStatistic(
                route,
                stop,
                LocalDate.of(2026, 1, 1).plusDays(dayOffset),
                trips,
                delayed,
                cancelled,
                delay,
                delay,
                delay);
    }
}
