package ru.spb.transit.integration.aggregator.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Objects;

public class ApiRecord {
    private final String source;
    private final Instant timestamp;
    private final JsonNode data;

    public ApiRecord(String source, Instant timestamp, JsonNode data) {
        this.source = Objects.requireNonNull(source, "source");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.data = Objects.requireNonNull(data, "data");
    }

    public String getSource() {
        return source;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public JsonNode getData() {
        return data;
    }
}
