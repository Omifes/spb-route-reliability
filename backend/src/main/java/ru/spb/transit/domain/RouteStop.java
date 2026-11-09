package ru.spb.transit.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "route_stops")
public class RouteStop {

    @EmbeddedId
    private RouteStopId id;

    @MapsId("routeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stop_id", nullable = false)
    private Stop stop;

    protected RouteStop() {
    }

    public RouteStop(Route route, short directionId, int sequenceNumber, Stop stop) {
        this.route = Objects.requireNonNull(route, "route");
        this.stop = Objects.requireNonNull(stop, "stop");
        this.id = new RouteStopId(route.getId(), directionId, sequenceNumber);
    }

    public RouteStopId getId() {
        return id;
    }

    public Route getRoute() {
        return route;
    }

    public Stop getStop() {
        return stop;
    }

    public short getDirectionId() {
        return id.getDirectionId();
    }

    public int getSequenceNumber() {
        return id.getSequenceNumber();
    }
}
