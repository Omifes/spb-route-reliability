package ru.spb.transit.integration.aggregator.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.spb.transit.integration.aggregator.model.ApiRecord;

import java.time.Instant;

public class MockTransportApiClient implements ApiClient {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "mock-transport";
    }

    @Override
    public ApiRecord fetch() throws Exception {
        String json = """
                {
                  "routes": [
                    {
                      "routeNumber": "39",
                      "vehicleId": "BUS-001",
                      "stopId": "STOP-001",
                      "stopName": "Невский проспект",
                      "latitude": 59.9343,
                      "longitude": 30.3351,
                      "delayMinutes": 3
                    },
                    {
                      "routeNumber": "7",
                      "vehicleId": "BUS-002",
                      "stopId": "STOP-002",
                      "stopName": "Площадь Восстания",
                      "latitude": 59.9311,
                      "longitude": 30.3609,
                      "delayMinutes": 5
                    }
                  ]
                }
                """;

        JsonNode data = objectMapper.readTree(json);
        return new ApiRecord(getName(), Instant.now(), data);
    }
}
