package ru.spb.transit.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.spb.transit.domain.HistoricalStatistic;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HistoricalStatisticRepository extends JpaRepository<HistoricalStatistic, Long> {

    Optional<HistoricalStatistic> findByRouteIdAndStopIdAndStatDate(Long routeId, Long stopId, LocalDate statDate);

    @EntityGraph(attributePaths = {"route", "stop"})
    List<HistoricalStatistic> findByRouteIdAndStopIdAndStatDateBetweenOrderByStatDateAsc(
            Long routeId, Long stopId, LocalDate from, LocalDate to);

    List<HistoricalStatistic> findByRouteIdAndStatDateBetweenOrderByStatDateAsc(
            Long routeId, LocalDate from, LocalDate to);
}
