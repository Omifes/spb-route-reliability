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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "historical_statistics")
public class HistoricalStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stop_id", nullable = false)
    private Stop stop;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "trips_count", nullable = false)
    private int tripsCount;

    @Column(name = "delayed_trips", nullable = false)
    private int delayedTrips;

    @Column(name = "cancelled_trips", nullable = false)
    private int cancelledTrips;

    @Column(name = "average_delay", precision = 8, scale = 2)
    private BigDecimal averageDelay;

    @Column(name = "median_delay", precision = 8, scale = 2)
    private BigDecimal medianDelay;

    @Column(name = "max_delay", precision = 8, scale = 2)
    private BigDecimal maxDelay;

    @CreationTimestamp
    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    protected HistoricalStatistic() {
    }

    public HistoricalStatistic(Route route, Stop stop, LocalDate statDate,
                               int tripsCount, int delayedTrips, int cancelledTrips,
                               BigDecimal averageDelay, BigDecimal medianDelay, BigDecimal maxDelay) {
        this.route = Objects.requireNonNull(route, "route");
        this.stop = Objects.requireNonNull(stop, "stop");
        this.statDate = Objects.requireNonNull(statDate, "statDate");
        this.tripsCount = tripsCount;
        this.delayedTrips = delayedTrips;
        this.cancelledTrips = cancelledTrips;
        this.averageDelay = averageDelay;
        this.medianDelay = medianDelay;
        this.maxDelay = maxDelay;
    }

    public Long getId() {
        return id;
    }

    public Route getRoute() {
        return route;
    }

    public Stop getStop() {
        return stop;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public int getTripsCount() {
        return tripsCount;
    }

    public int getDelayedTrips() {
        return delayedTrips;
    }

    public int getCancelledTrips() {
        return cancelledTrips;
    }

    public BigDecimal getAverageDelay() {
        return averageDelay;
    }

    public BigDecimal getMedianDelay() {
        return medianDelay;
    }

    public BigDecimal getMaxDelay() {
        return maxDelay;
    }

    public Instant getComputedAt() {
        return computedAt;
    }
}
