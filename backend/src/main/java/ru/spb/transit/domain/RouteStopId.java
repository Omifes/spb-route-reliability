package ru.spb.transit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RouteStopId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "direction_id", nullable = false)
    private short directionId;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    public RouteStopId() {
    }

    public RouteStopId(Long routeId, short directionId, int sequenceNumber) {
        this.routeId = routeId;
        this.directionId = directionId;
        this.sequenceNumber = sequenceNumber;
    }

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public short getDirectionId() {
        return directionId;
    }

    public void setDirectionId(short directionId) {
        this.directionId = directionId;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RouteStopId that)) {
            return false;
        }
        return directionId == that.directionId
                && sequenceNumber == that.sequenceNumber
                && Objects.equals(routeId, that.routeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(routeId, directionId, sequenceNumber);
    }
}
