package ru.spb.transit.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.spb.transit.domain.HistoricalStatistic;
import ru.spb.transit.domain.SavedTrip;
import ru.spb.transit.reliability.ReliabilityCalculator;
import ru.spb.transit.reliability.ReliabilitySummary;
import ru.spb.transit.repository.HistoricalStatisticRepository;
import ru.spb.transit.web.dto.ReliabilityResponse;
import ru.spb.transit.web.error.BusinessValidationException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ReliabilityService {

    public static final int DEFAULT_PERIOD_DAYS = 30;
    public static final int MAX_PERIOD_DAYS = 365;

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");

    private final TripService tripService;
    private final HistoricalStatisticRepository historicalStatisticRepository;
    private final ReliabilityCalculator reliabilityCalculator;

    public ReliabilityService(TripService tripService,
                              HistoricalStatisticRepository historicalStatisticRepository,
                              ReliabilityCalculator reliabilityCalculator) {
        this.tripService = tripService;
        this.historicalStatisticRepository = historicalStatisticRepository;
        this.reliabilityCalculator = reliabilityCalculator;
    }

    public ReliabilityResponse getReliability(String email, Long tripId, int periodDays) {
        if (periodDays < 1 || periodDays > MAX_PERIOD_DAYS) {
            throw new BusinessValidationException(
                    "Период оценки должен быть от 1 до " + MAX_PERIOD_DAYS + " дней");
        }

        SavedTrip trip = tripService.findOwnedTrip(email, tripId);

        LocalDate to = LocalDate.now(MOSCOW);
        LocalDate from = to.minusDays(periodDays - 1L);

        // Для поездки важна надёжность маршрута именно на остановке назначения.
        List<HistoricalStatistic> statistics = historicalStatisticRepository
                .findByRouteIdAndStopIdAndStatDateBetweenOrderByStatDateAsc(
                        trip.getRoute().getId(), trip.getToStop().getId(), from, to);

        ReliabilitySummary summary = reliabilityCalculator.calculate(statistics, periodDays);

        return new ReliabilityResponse(
                trip.getId(),
                periodDays,
                summary.averageDelayMinutes(),
                summary.delayedTripsPercent(),
                summary.cancellationPercent(),
                summary.tripsCount(),
                summary.riskLevel().name().toLowerCase(Locale.ROOT),
                summary.dataSufficient());
    }
}
