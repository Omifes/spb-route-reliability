package ru.spb.transit.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.spb.transit.domain.Route;
import ru.spb.transit.domain.SavedTrip;
import ru.spb.transit.domain.Stop;
import ru.spb.transit.domain.User;
import ru.spb.transit.repository.RouteRepository;
import ru.spb.transit.repository.SavedTripRepository;
import ru.spb.transit.repository.StopRepository;
import ru.spb.transit.repository.UserRepository;
import ru.spb.transit.web.dto.CreateTripRequest;
import ru.spb.transit.web.dto.TripResponse;
import ru.spb.transit.web.error.BusinessValidationException;
import ru.spb.transit.web.error.CurrentUserNotFoundException;
import ru.spb.transit.web.error.TripNotFoundException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TripService {

    private final UserRepository userRepository;
    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final SavedTripRepository savedTripRepository;

    public TripService(UserRepository userRepository,
                       RouteRepository routeRepository,
                       StopRepository stopRepository,
                       SavedTripRepository savedTripRepository) {
        this.userRepository = userRepository;
        this.routeRepository = routeRepository;
        this.stopRepository = stopRepository;
        this.savedTripRepository = savedTripRepository;
    }

    public List<TripResponse> findAllForUser(String email) {
        User user = requireUser(email);
        return savedTripRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TripResponse create(String email, CreateTripRequest request) {
        User user = requireUser(email);

        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new BusinessValidationException("Название поездки обязательно");
        }
        if (request.fromStopId().equals(request.toStopId())) {
            throw new BusinessValidationException(
                    "Остановка отправления и остановка назначения должны различаться");
        }

        Route route = routeRepository.findByExternalId(request.routeId())
                .orElseThrow(() -> new BusinessValidationException(
                        "Маршрут с идентификатором " + request.routeId() + " не найден"));
        Stop fromStop = stopRepository.findByExternalId(request.fromStopId())
                .orElseThrow(() -> new BusinessValidationException(
                        "Остановка отправления с идентификатором " + request.fromStopId() + " не найдена"));
        Stop toStop = stopRepository.findByExternalId(request.toStopId())
                .orElseThrow(() -> new BusinessValidationException(
                        "Остановка назначения с идентификатором " + request.toStopId() + " не найдена"));

        SavedTrip trip = new SavedTrip(
                user, route, (short) 0, fromStop, toStop, name, request.requiredArrivalTime());
        return toResponse(savedTripRepository.saveAndFlush(trip));
    }

    public SavedTrip findOwnedTrip(String email, Long tripId) {
        User user = requireUser(email);
        return savedTripRepository.findByIdAndUserId(tripId, user.getId())
                .orElseThrow(() -> new TripNotFoundException(tripId));
    }

    @Transactional
    public void delete(String email, Long tripId) {
        SavedTrip trip = findOwnedTrip(email, tripId);
        savedTripRepository.delete(trip);
    }

    private User requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(CurrentUserNotFoundException::new);
    }

    private TripResponse toResponse(SavedTrip trip) {
        return new TripResponse(
                trip.getId(),
                trip.getName(),
                trip.getRoute().getExternalId(),
                trip.getFromStop().getExternalId(),
                trip.getToStop().getExternalId(),
                trip.getArrivalTime());
    }
}
