package ru.spb.transit.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import ru.spb.transit.domain.TransportEventType;
import ru.spb.transit.domain.TransportUpdateStatus;

import java.time.Instant;

public record TransportUpdateResponse(
        Long id,
        String source,
        String routeExternalId,
        String stopExternalId,
        String tripExternalId,
        String vehicleExternalId,
        TransportEventType eventType,
        Instant observedAt,
        Instant scheduledAt,
        Instant actualAt,
        Integer delaySeconds,
        TransportUpdateStatus status,
        JsonNode payload,
        Instant receivedAt
) {
}