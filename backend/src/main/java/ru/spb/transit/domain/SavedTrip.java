package ru.spb.transit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "saved_trips")
public class SavedTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(name = "direction_id", nullable = false)
    private short directionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_stop_id", nullable = false)
    private Stop fromStop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_stop_id", nullable = false)
    private Stop toStop;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SavedTrip() {
    }

    public SavedTrip(User user, Route route, short directionId, Stop fromStop, Stop toStop,
                     String name, LocalTime arrivalTime) {
        this.user = Objects.requireNonNull(user, "user");
        this.route = Objects.requireNonNull(route, "route");
        this.directionId = directionId;
        this.fromStop = Objects.requireNonNull(fromStop, "fromStop");
        this.toStop = Objects.requireNonNull(toStop, "toStop");
        this.name = Objects.requireNonNull(name, "name");
        this.arrivalTime = Objects.requireNonNull(arrivalTime, "arrivalTime");
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Route getRoute() {
        return route;
    }

    public short getDirectionId() {
        return directionId;
    }

    public void setDirectionId(short directionId) {
        this.directionId = directionId;
    }

    public Stop getFromStop() {
        return fromStop;
    }

    public void setFromStop(Stop fromStop) {
        this.fromStop = fromStop;
    }

    public Stop getToStop() {
        return toStop;
    }

    public void setToStop(Stop toStop) {
        this.toStop = toStop;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
