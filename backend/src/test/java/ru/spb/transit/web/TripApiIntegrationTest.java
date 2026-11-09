package ru.spb.transit.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.spb.transit.domain.HistoricalStatistic;
import ru.spb.transit.domain.Route;
import ru.spb.transit.domain.RouteType;
import ru.spb.transit.domain.SavedTrip;
import ru.spb.transit.domain.Stop;
import ru.spb.transit.domain.TransportEventType;
import ru.spb.transit.domain.TransportUpdate;
import ru.spb.transit.domain.TransportUpdateStatus;
import ru.spb.transit.domain.User;
import ru.spb.transit.repository.HistoricalStatisticRepository;
import ru.spb.transit.repository.RouteRepository;
import ru.spb.transit.repository.SavedTripRepository;
import ru.spb.transit.repository.StopRepository;
import ru.spb.transit.repository.TransportUpdateRepository;
import ru.spb.transit.repository.UserRepository;
import ru.spb.transit.support.PostgresIntegrationTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class TripApiIntegrationTest extends PostgresIntegrationTest {

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private StopRepository stopRepository;

    @Autowired
    private SavedTripRepository savedTripRepository;

    @Autowired
    private HistoricalStatisticRepository historicalStatisticRepository;

    @Autowired
    private TransportUpdateRepository transportUpdateRepository;

    private User alice;
    private User bob;
    private Route route;
    private Stop fromStop;
    private Stop toStop;

    @BeforeEach
    void setUp() {
        alice = userRepository.saveAndFlush(
                new User("alice@example.com", passwordEncoder.encode("secret"), "Alice"));
        bob = userRepository.saveAndFlush(
                new User("bob@example.com", passwordEncoder.encode("secret"), "Bob"));
        route = routeRepository.saveAndFlush(new Route("321", "Трамвай 21", RouteType.TRAM));
        fromStop = stopRepository.saveAndFlush(
                new Stop("12345", "Невский проспект", new BigDecimal("59.934280"), new BigDecimal("30.335098")));
        toStop = stopRepository.saveAndFlush(
                new Stop("67890", "Университет", new BigDecimal("59.943170"), new BigDecimal("30.298611")));
    }

    @Test
    void requestWithoutCredentialsIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/trips"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void basicAuthenticationWorks() throws Exception {
        mockMvc.perform(get("/api/trips").with(httpBasic("alice@example.com", "secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trips").isArray());
    }

    @Test
    void corsPreflightFromAllowedOriginIsPermitted() throws Exception {
        mockMvc.perform(options("/api/trips")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5500")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5500"));
    }

    @Test
    void corsHeadersAreReturnedForAuthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/trips")
                        .with(httpBasic("alice@example.com", "secret"))
                        .header(HttpHeaders.ORIGIN, "http://localhost:5500"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5500"));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    void listReturnsOnlyTripsOfCurrentUser() throws Exception {
        saveTrip(alice, "Дом — Университет", LocalTime.of(8, 40));
        saveTrip(bob, "Чужая поездка", LocalTime.of(9, 0));

        mockMvc.perform(get("/api/trips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trips.length()").value(1))
                .andExpect(jsonPath("$.trips[0].name").value("Дом — Университет"))
                .andExpect(jsonPath("$.trips[0].routeId").value("321"))
                .andExpect(jsonPath("$.trips[0].fromStopId").value("12345"))
                .andExpect(jsonPath("$.trips[0].toStopId").value("67890"))
                .andExpect(jsonPath("$.trips[0].requiredArrivalTime").value("08:40"));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    void createTripReturnsCreatedTrip() throws Exception {
        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson("Дом — Университет", "321", "12345", "67890", "08:40")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Дом — Университет"))
                .andExpect(jsonPath("$.routeId").value("321"))
                .andExpect(jsonPath("$.requiredArrivalTime").value("08:40"));

        assertThat(savedTripRepository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    void createTripRejectsUnknownRoute() throws Exception {
        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson("Поездка", "unknown", "12345", "67890", "08:40")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Маршрут")));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    void createTripRejectsSameStops() throws Exception {
        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson("Поездка", "321", "12345", "12345", "08:40")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("различаться")));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    void createTripRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson("   ", "321", "12345", "67890", "08:40")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    void createTripRejectsInvalidTimeFormat() throws Exception {
        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson("Поездка", "321", "12345", "67890", "25:00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    void statusUsesCurrentDestinationUpdate() throws Exception {
        LocalTime requiredArrival = LocalTime.now(MOSCOW).plusMinutes(30).withSecond(0).withNano(0);
        SavedTrip trip = saveTrip(alice, "На учёбу", requiredArrival);

        Instant now = Instant.now();
        TransportUpdate update = new TransportUpdate(
                "gtfs-realtime", TransportEventType.TRIP_UPDATE, now,
                objectMapper.createObjectNode().put("trip_id", "trip-1"));
        update.setRouteExternalId("321");
        update.setStopExternalId("67890");
        update.setScheduledAt(now.plus(5, ChronoUnit.MINUTES));
        update.setActualAt(now.plus(7, ChronoUnit.MINUTES));
        update.setDelaySeconds(120);
        update.setStatus(TransportUpdateStatus.DELAYED);
        transportUpdateRepository.saveAndFlush(update);

        mockMvc.perform(get("/api/trips/{id}/status", trip.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(trip.getId()))
                .andExpect(jsonPath("$.route").value("321"))
                .andExpect(jsonPath("$.nextStop").value("Университет"))
                .andExpect(jsonPath("$.etaMinutes").isNumber())
                .andExpect(jsonPath("$.arrivalTime").isString())
                .andExpect(jsonPath("$.delayMinutes").value(2))
                .andExpect(jsonPath("$.willArriveOnTime").value(true))
                .andExpect(jsonPath("$.dataStatus").value("current"))
                .andExpect(jsonPath("$.dataUpdatedAt").isString());
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    void statusMarksOldUpdateAsStale() throws Exception {
        SavedTrip trip = saveTrip(alice, "На учёбу", LocalTime.now(MOSCOW).plusMinutes(30));

        Instant now = Instant.now();
        TransportUpdate update = new TransportUpdate(
                "gtfs-realtime", TransportEventType.TRIP_UPDATE,
                now.minus(20, ChronoUnit.MINUTES),
                objectMapper.createObjectNode());
        update.setRouteExternalId("321");
        update.setStopExternalId("67890");
        update.setActualAt(now.plus(7, ChronoUnit.MINUTES));
        update.setDelaySeconds(60);
        update.setStatus(TransportUpdateStatus.DELAYED);
        transportUpdateRepository.saveAndFlush(update);

        mockMvc.perform(get("/api/trips/{id}/status", trip.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataStatus").value("stale"))
                .andExpect(jsonPath("$.delayMinutes").value(1));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    void statusReportsUnavailableWhenThereAreNoUpdates() throws Exception {
        SavedTrip trip = saveTrip(alice, "На учёбу", LocalTime.now(MOSCOW).plusMinutes(30));

        mockMvc.perform(get("/api/trips/{id}/status", trip.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataStatus").value("unavailable"))
                .andExpect(jsonPath("$.etaMinutes").value(nullValue()))
                .andExpect(jsonPath("$.arrivalTime").value(nullValue()))
                .andExpect(jsonPath("$.willArriveOnTime").value(nullValue()))
                .andExpect(jsonPath("$.nextStop").value("Университет"));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    void statusReportsCancelledTripAsNotArrivingOnTime() throws Exception {
        SavedTrip trip = saveTrip(alice, "Отменённая поездка", LocalTime.now(MOSCOW).plusMinutes(30));

        TransportUpdate update = new TransportUpdate(
                "gtfs-realtime", TransportEventType.TRIP_UPDATE, Instant.now(),
                objectMapper.createObjectNode());
        update.setRouteExternalId("321");
        update.setStopExternalId("67890");
        update.setStatus(TransportUpdateStatus.CANCELLED);
        transportUpdateRepository.saveAndFlush(update);

        mockMvc.perform(get("/api/trips/{id}/status", trip.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataStatus").value("current"))
                .andExpect(jsonPath("$.willArriveOnTime").value(false))
                .andExpect(jsonPath("$.arrivalTime").value(nullValue()))
                .andExpect(jsonPath("$.etaMinutes").value(nullValue()))
                .andExpect(jsonPath("$.delayMinutes").value(nullValue()));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    void statusOfAnotherUsersTripReturnsNotFound() throws Exception {
        SavedTrip bobTrip = saveTrip(bob, "Чужая поездка", LocalTime.of(9, 0));

        mockMvc.perform(get("/api/trips/{id}/status", bobTrip.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    void reliabilityAggregatesDestinationStopStatistics() throws Exception {
        SavedTrip trip = saveTrip(alice, "На учёбу", LocalTime.of(8, 40));
        LocalDate today = LocalDate.now(MOSCOW);

        for (int i = 0; i < 10; i++) {
            historicalStatisticRepository.saveAndFlush(new HistoricalStatistic(
                    route, toStop, today.minusDays(i),
                    10, 2, 0,
                    new BigDecimal("5.00"), new BigDecimal("5.00"), new BigDecimal("8.00")));
        }

        mockMvc.perform(get("/api/trips/{id}/reliability", trip.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(trip.getId()))
                .andExpect(jsonPath("$.periodDays").value(30))
                .andExpect(jsonPath("$.tripsCount").value(100))
                .andExpect(jsonPath("$.averageDelayMinutes").value(5.0))
                .andExpect(jsonPath("$.delayedTripsPercent").value(20.0))
                .andExpect(jsonPath("$.riskLevel").value("medium"))
                .andExpect(jsonPath("$.dataSufficient").value(true));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    void reliabilityReportsInsufficientData() throws Exception {
        SavedTrip trip = saveTrip(alice, "На учёбу", LocalTime.of(8, 40));

        mockMvc.perform(get("/api/trips/{id}/reliability", trip.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("unknown"))
                .andExpect(jsonPath("$.tripsCount").value(0))
                .andExpect(jsonPath("$.averageDelayMinutes").value(nullValue()))
                .andExpect(jsonPath("$.dataSufficient").value(false));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    void reliabilityRejectsInvalidPeriod() throws Exception {
        SavedTrip trip = saveTrip(alice, "На учёбу", LocalTime.of(8, 40));

        mockMvc.perform(get("/api/trips/{id}/reliability", trip.getId())
                        .param("periodDays", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    void deleteRemovesOwnTrip() throws Exception {
        SavedTrip trip = saveTrip(alice, "Удаляемая поездка", LocalTime.of(8, 40));

        mockMvc.perform(delete("/api/trips/{id}", trip.getId()))
                .andExpect(status().isNoContent());

        savedTripRepository.flush();
        assertThat(savedTripRepository.findById(trip.getId())).isEmpty();
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    void deleteOfAnotherUsersTripReturnsNotFound() throws Exception {
        SavedTrip bobTrip = saveTrip(bob, "Чужая поездка", LocalTime.of(9, 0));

        mockMvc.perform(delete("/api/trips/{id}", bobTrip.getId()))
                .andExpect(status().isNotFound());

        assertThat(savedTripRepository.findById(bobTrip.getId())).isPresent();
    }

    private SavedTrip saveTrip(User user, String name, LocalTime arrivalTime) {
        return savedTripRepository.saveAndFlush(
                new SavedTrip(user, route, (short) 0, fromStop, toStop, name, arrivalTime));
    }

    private String tripJson(String name, String routeId, String fromStopId,
                            String toStopId, String requiredArrivalTime) throws Exception {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("name", name);
        node.put("routeId", routeId);
        node.put("fromStopId", fromStopId);
        node.put("toStopId", toStopId);
        node.put("requiredArrivalTime", requiredArrivalTime);
        return objectMapper.writeValueAsString(node);
    }
}
