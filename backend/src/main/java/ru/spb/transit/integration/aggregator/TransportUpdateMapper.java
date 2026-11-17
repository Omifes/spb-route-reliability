package ru.spb.transit.integration.aggregator;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import ru.spb.transit.domain.TransportEventType;
import ru.spb.transit.domain.TransportUpdate;
import ru.spb.transit.integration.aggregator.model.ApiRecord;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class TransportUpdateMapper {
    public List<TransportUpdate> toEntities(ApiRecord record) {
        List<TransportUpdate> updates = new ArrayList<>();
        JsonNode routes = record.getData().path("routes");

        if (!routes.isArray()) {
            return updates;
        }

        for (JsonNode route : routes) {
            Instant observedAt = record.getTimestamp();
            String timestamp = route.path("timestamp").asText(null);
            if (timestamp != null && !timestamp.isBlank()) {
                observedAt = Instant.parse(timestamp);
            }

            JsonNode payload = route.deepCopy();
            TransportUpdate update = new TransportUpdate(
                    record.getSource(),
                    TransportEventType.VEHICLE_POSITION,
                    observedAt,
                    payload
            );

            update.setRouteExternalId(textOrNull(route, "routeNumber"));
            update.setVehicleExternalId(textOrNull(route, "vehicleId"));
            update.setStopExternalId(textOrNull(route, "stopId"));

            if (route.hasNonNull("delayMinutes")) {
                update.setDelaySeconds(route.path("delayMinutes").asInt() * 60);
            }

            updates.add(update);
        }

        return updates;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
