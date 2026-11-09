package ru.spb.transit.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import ru.spb.transit.domain.HistoricalStatistic;
import ru.spb.transit.domain.Route;
import ru.spb.transit.domain.RouteStop;
import ru.spb.transit.domain.RouteType;
import ru.spb.transit.domain.SavedTrip;
import ru.spb.transit.domain.Stop;
import ru.spb.transit.domain.TransportEventType;
import ru.spb.transit.domain.TransportUpdate;
import ru.spb.transit.domain.TransportUpdateStatus;
import ru.spb.transit.domain.User;
import ru.spb.transit.support.PostgresIntegrationTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class DatabaseIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private StopRepository stopRepository;

    @Autowired
    private RouteStopRepository routeStopRepository;

    @Autowired
    private SavedTripRepository savedTripRepository;

    @Autowired
    private HistoricalStatisticRepository historicalStatisticRepository;

    @Autowired
    private TransportUpdateRepository transportUpdateRepository;

    @Test
    void flywayCreatesCoreTablesAndAppliesBothMigrations() {
        List<String> tables = jdbcTemplate.queryForList("""
                select table_name
                from information_schema.tables
                where table_schema = 'public' and table_type = 'BASE TABLE'
                """, String.class);

        assertThat(tables).contains(
                "users", "routes", "stops", "route_stops",
                "saved_trips", "historical_statistics", "transport_updates");

        Integer applied = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true", Integer.class);
        assertThat(applied).isEqualTo(2);
    }

    @Test
    void savesTripAndReturnsOnlyTripsOfRequestedUser() {
        User alice = userRepository.saveAndFlush(new User("alice@example.com", "hash", "Alice"));
        User bob = userRepository.saveAndFlush(new User("bob@example.com", "hash", "Bob"));
        Route route = routeRepository.saveAndFlush(new Route("route-7", "Трамвай 7", RouteType.TRAM));
        Stop from = stopRepository.saveAndFlush(stop("stop-1", "Невский проспект"));
        Stop to = stopRepository.saveAndFlush(stop("stop-2", "Площадь Ленина"));

        SavedTrip trip = savedTripRepository.saveAndFlush(
                new SavedTrip(alice, route, (short) 0, from, to, "Дом — Работа", LocalTime.of(8, 40)));

        List<SavedTrip> aliceTrips = savedTripRepository.findByUserIdOrderByCreatedAtDesc(alice.getId());

        assertThat(aliceTrips).hasSize(1);
        assertThat(aliceTrips.get(0).getId()).isEqualTo(trip.getId());
        assertThat(aliceTrips.get(0).getRoute().getName()).isEqualTo("Трамвай 7");
        assertThat(aliceTrips.get(0).getFromStop().getName()).isEqualTo("Невский проспект");
        assertThat(aliceTrips.get(0).getArrivalTime()).isEqualTo(LocalTime.of(8, 40));

        assertThat(savedTripRepository.findByIdAndUserId(trip.getId(), alice.getId())).isPresent();
        assertThat(savedTripRepository.findByIdAndUserId(trip.getId(), bob.getId())).isEmpty();
    }

    @Test
    void emailUniquenessIsCaseInsensitive() {
        userRepository.saveAndFlush(new User("Test@Example.com", "hash", "Test"));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into users (email) values (?)", "TEST@EXAMPLE.COM"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void routeStopsAreReturnedInSequenceAndRouteCanBeFoundByStop() {
        Route route = routeRepository.saveAndFlush(new Route("route-3", "Автобус 3", RouteType.BUS));
        Stop first = stopRepository.saveAndFlush(stop("stop-a", "Первая"));
        Stop second = stopRepository.saveAndFlush(stop("stop-b", "Вторая"));

        routeStopRepository.saveAndFlush(new RouteStop(route, (short) 0, 20, second));
        routeStopRepository.saveAndFlush(new RouteStop(route, (short) 0, 10, first));

        List<RouteStop> ordered = routeStopRepository.findRouteStops(route.getId(), (short) 0);

        assertThat(ordered).extracting(RouteStop::getSequenceNumber).containsExactly(10, 20);
        assertThat(ordered).extracting(rs -> rs.getStop().getName())
                .containsExactly("Первая", "Вторая");
        assertThat(routeStopRepository.findRoutesByStopId(first.getId()))
                .extracting(rs -> rs.getRoute().getExternalId())
                .containsExactly("route-3");
    }

    @Test
    void databaseRejectsInvalidCoordinates() {
        Stop invalid = new Stop("bad-stop", "Некорректная", BigDecimal.valueOf(91), BigDecimal.valueOf(30));

        assertThatThrownBy(() -> stopRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingUserCascadesToSavedTrips() {
        User user = userRepository.saveAndFlush(new User("cascade@example.com", "hash", "Cascade"));
        Route route = routeRepository.saveAndFlush(new Route("route-9", "Троллейбус 9", RouteType.TROLLEYBUS));
        Stop from = stopRepository.saveAndFlush(stop("stop-c", "Отправление"));
        Stop to = stopRepository.saveAndFlush(stop("stop-d", "Назначение"));
        SavedTrip trip = savedTripRepository.saveAndFlush(
                new SavedTrip(user, route, (short) 0, from, to, "Поездка", LocalTime.NOON));

        // Detach the trip so Hibernate does not try to re-attach the user being removed.
        // The actual cleanup must happen in PostgreSQL via ON DELETE CASCADE.
        entityManager.clear();
        userRepository.deleteById(user.getId());
        userRepository.flush();

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from saved_trips where id = ?", Integer.class, trip.getId());
        assertThat(count).isZero();
    }

    @Test
    void historicalStatisticsAreUniquePerRouteStopAndDateAndReadableByPeriod() {
        Route route = routeRepository.saveAndFlush(new Route("route-5", "Автобус 5", RouteType.BUS));
        Stop stop = stopRepository.saveAndFlush(stop("stop-e", "Статистика"));
        LocalDate day = LocalDate.of(2026, 10, 1);

        historicalStatisticRepository.saveAndFlush(stat(route, stop, day, 100, 8, 1, "3.50"));
        historicalStatisticRepository.saveAndFlush(stat(route, stop, day.plusDays(1), 120, 20, 0, "5.00"));

        List<HistoricalStatistic> period = historicalStatisticRepository
                .findByRouteIdAndStopIdAndStatDateBetweenOrderByStatDateAsc(
                        route.getId(), stop.getId(), day, day.plusDays(1));

        assertThat(period).hasSize(2);
        assertThat(period).extracting(HistoricalStatistic::getTripsCount).containsExactly(100, 120);

        assertThatThrownBy(() -> historicalStatisticRepository.saveAndFlush(
                stat(route, stop, day, 10, 1, 0, "1.00")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void transportUpdateStoresRawJsonAndCanBePurged() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        TransportUpdate update = new TransportUpdate(
                "gtfs-realtime",
                TransportEventType.TRIP_UPDATE,
                Instant.now(),
                objectMapper.readTree("{\"trip_id\":\"trip-42\",\"delay\":90}"));
        update.setRouteExternalId("route-42");
        update.setStopExternalId("stop-42");
        update.setStatus(TransportUpdateStatus.DELAYED);
        update.setDelaySeconds(90);

        TransportUpdate saved = transportUpdateRepository.saveAndFlush(update);

        String tripId = jdbcTemplate.queryForObject(
                "select payload ->> 'trip_id' from transport_updates where id = ?",
                String.class, saved.getId());
        assertThat(tripId).isEqualTo("trip-42");

        TransportUpdate old = new TransportUpdate(
                "gtfs-realtime",
                TransportEventType.VEHICLE_POSITION,
                Instant.now().minus(2, ChronoUnit.DAYS),
                objectMapper.readTree("{}"));
        transportUpdateRepository.saveAndFlush(old);

        int deleted = transportUpdateRepository.deleteOlderThan(Instant.now().minus(1, ChronoUnit.DAYS));

        assertThat(deleted).isEqualTo(1);
    }

    private Stop stop(String externalId, String name) {
        return new Stop(externalId, name, new BigDecimal("59.934280"), new BigDecimal("30.335098"));
    }

    private HistoricalStatistic stat(Route route, Stop stop, LocalDate date,
                                     int trips, int delayed, int cancelled, String averageDelay) {
        return new HistoricalStatistic(route, stop, date, trips, delayed, cancelled,
                new BigDecimal(averageDelay), new BigDecimal(averageDelay), new BigDecimal(averageDelay));
    }
}
