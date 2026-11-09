package ru.spb.transit.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.spb.transit.domain.RouteStop;
import ru.spb.transit.domain.SavedTrip;
import ru.spb.transit.domain.TransportUpdate;
import ru.spb.transit.domain.TransportUpdateStatus;
import ru.spb.transit.repository.RouteStopRepository;
import ru.spb.transit.repository.TransportUpdateRepository;
import ru.spb.transit.web.dto.TripStatusResponse;
import ru.spb.transit.web.dto.TripStatusResponse.DataStatus;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TripStatusService {

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");

    private final TripService tripService;
    private final TransportUpdateRepository transportUpdateRepository;
    private final RouteStopRepository routeStopRepository;
    private final Duration currentWindow;
    private final Duration staleWindow;

    public TripStatusService(TripService tripService,
                             TransportUpdateRepository transportUpdateRepository,
                             RouteStopRepository routeStopRepository,
                             @Value("${app.status.current-window:PT5M}") Duration currentWindow,
                             @Value("${app.status.stale-window:PT60M}") Duration staleWindow) {
        this.tripService = tripService;
        this.transportUpdateRepository = transportUpdateRepository;
        this.routeStopRepository = routeStopRepository;
        this.currentWindow = currentWindow;
        this.staleWindow = staleWindow;
    }

    public TripStatusResponse getStatus(String email, Long tripId) {
        SavedTrip trip = tripService.findOwnedTrip(email, tripId);
        Instant now = Instant.now();

        Optional<TransportUpdate> destinationUpdate = transportUpdateRepository
                .findFirstByRouteExternalIdAndStopExternalIdOrderByObservedAtDesc(
                        trip.getRoute().getExternalId(), trip.getToStop().getExternalId());
        Optional<TransportUpdate> routeUpdate = transportUpdateRepository
                .findFirstByRouteExternalIdOrderByObservedAtDesc(trip.getRoute().getExternalId());

        TransportUpdate update = destinationUpdate.or(() -> routeUpdate).orElse(null);
        DataStatus dataStatus = dataStatus(update, now);
        boolean dataUsable = dataStatus != DataStatus.UNAVAILABLE;

        TransportUpdate arrivalSource = destinationUpdate.orElse(null);
        Instant predictedArrival = dataUsable ? predictedArrival(arrivalSource) : null;
        boolean cancelled = dataUsable && update != null
                && update.getStatus() == TransportUpdateStatus.CANCELLED;

        Integer delayMinutes = dataUsable && !cancelled ? delayMinutes(update) : null;
        Integer etaMinutes = predictedArrival == null ? null : minutesUntil(now, predictedArrival);
        LocalTime arrivalTime = predictedArrival == null
                ? null
                : predictedArrival.atZone(MOSCOW).toLocalTime();
        Boolean willArriveOnTime;
        if (predictedArrival == null) {
            willArriveOnTime = cancelled ? Boolean.FALSE : null;
        } else {
            willArriveOnTime = !predictedArrival.isAfter(
                    requiredArrivalInstant(trip.getArrivalTime(), now));
        }
        LocalDateTime dataUpdatedAt = update == null
                ? null
                : update.getObservedAt().atZone(MOSCOW).toLocalDateTime();

        return new TripStatusResponse(
                trip.getId(),
                trip.getRoute().getExternalId(),
                nextStop(trip, update),
                etaMinutes,
                arrivalTime,
                trip.getArrivalTime(),
                delayMinutes,
                willArriveOnTime,
                dataStatus,
                dataUpdatedAt);
    }

    private DataStatus dataStatus(TransportUpdate update, Instant now) {
        if (update == null) {
            return DataStatus.UNAVAILABLE;
        }
        Duration age = Duration.between(update.getObservedAt(), now);
        if (age.isNegative() || age.compareTo(currentWindow) <= 0) {
            return DataStatus.CURRENT;
        }
        if (age.compareTo(staleWindow) <= 0) {
            return DataStatus.STALE;
        }
        return DataStatus.UNAVAILABLE;
    }

    private Instant predictedArrival(TransportUpdate update) {
        if (update == null || update.getStatus() == TransportUpdateStatus.CANCELLED) {
            return null;
        }
        if (update.getActualAt() != null) {
            return update.getActualAt();
        }
        if (update.getScheduledAt() != null) {
            return update.getDelaySeconds() == null
                    ? update.getScheduledAt()
                    : update.getScheduledAt().plusSeconds(update.getDelaySeconds());
        }
        return null;
    }

    private Integer delayMinutes(TransportUpdate update) {
        if (update == null) {
            return null;
        }
        if (update.getDelaySeconds() != null) {
            return toMinutes(update.getDelaySeconds());
        }
        if (update.getActualAt() != null && update.getScheduledAt() != null) {
            return toMinutes(Duration.between(update.getScheduledAt(), update.getActualAt()).getSeconds());
        }
        return null;
    }

    private Integer minutesUntil(Instant now, Instant target) {
        return Math.max(0, toMinutes(Duration.between(now, target).getSeconds()));
    }

    private Integer toMinutes(long seconds) {
        return (int) Math.round(seconds / 60.0);
    }

    private Instant requiredArrivalInstant(LocalTime requiredArrivalTime, Instant now) {
        LocalDateTime nowLocal = now.atZone(MOSCOW).toLocalDateTime();
        LocalDateTime target = LocalDateTime.of(nowLocal.toLocalDate(), requiredArrivalTime);
        // Если время уже прошло более чем на 12 часов, считаем, что речь о следующем дне.
        if (target.isBefore(nowLocal.minusHours(12))) {
            target = target.plusDays(1);
        }
        return target.atZone(MOSCOW).toInstant();
    }

    private String nextStop(SavedTrip trip, TransportUpdate update) {
        if (update == null || update.getStopExternalId() == null) {
            return trip.getToStop().getName();
        }

        List<RouteStop> stops = routeStopRepository.findRouteStops(
                trip.getRoute().getId(), trip.getDirectionId());
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i).getStop().getExternalId().equals(update.getStopExternalId())) {
                if (i + 1 < stops.size()) {
                    return stops.get(i + 1).getStop().getName();
                }
                break;
            }
        }
        return trip.getToStop().getName();
    }
}
