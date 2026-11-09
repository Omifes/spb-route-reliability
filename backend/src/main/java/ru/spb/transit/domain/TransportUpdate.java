package ru.spb.transit.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "transport_updates")
public class TransportUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "route_external_id", length = 100)
    private String routeExternalId;

    @Column(name = "stop_external_id", length = 100)
    private String stopExternalId;

    @Column(name = "trip_external_id", length = 100)
    private String tripExternalId;

    @Column(name = "vehicle_external_id", length = 100)
    private String vehicleExternalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private TransportEventType eventType;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "actual_at")
    private Instant actualAt;

    @Column(name = "delay_seconds")
    private Integer delaySeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransportUpdateStatus status = TransportUpdateStatus.UNKNOWN;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    protected TransportUpdate() {
    }

    public TransportUpdate(String source, TransportEventType eventType, Instant observedAt, JsonNode payload) {
        this.source = Objects.requireNonNull(source, "source");
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        this.observedAt = Objects.requireNonNull(observedAt, "observedAt");
        this.payload = Objects.requireNonNull(payload, "payload");
    }

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRouteExternalId() {
        return routeExternalId;
    }

    public void setRouteExternalId(String routeExternalId) {
        this.routeExternalId = routeExternalId;
    }

    public String getStopExternalId() {
        return stopExternalId;
    }

    public void setStopExternalId(String stopExternalId) {
        this.stopExternalId = stopExternalId;
    }

    public String getTripExternalId() {
        return tripExternalId;
    }

    public void setTripExternalId(String tripExternalId) {
        this.tripExternalId = tripExternalId;
    }

    public String getVehicleExternalId() {
        return vehicleExternalId;
    }

    public void setVehicleExternalId(String vehicleExternalId) {
        this.vehicleExternalId = vehicleExternalId;
    }

    public TransportEventType getEventType() {
        return eventType;
    }

    public void setEventType(TransportEventType eventType) {
        this.eventType = eventType;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(Instant observedAt) {
        this.observedAt = observedAt;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Instant getActualAt() {
        return actualAt;
    }

    public void setActualAt(Instant actualAt) {
        this.actualAt = actualAt;
    }

    public Integer getDelaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(Integer delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    public TransportUpdateStatus getStatus() {
        return status;
    }

    public void setStatus(TransportUpdateStatus status) {
        this.status = status;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
