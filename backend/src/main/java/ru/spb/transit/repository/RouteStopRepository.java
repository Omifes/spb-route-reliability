package ru.spb.transit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.spb.transit.domain.RouteStop;
import ru.spb.transit.domain.RouteStopId;

import java.util.List;

public interface RouteStopRepository extends JpaRepository<RouteStop, RouteStopId> {

    @Query("""
            select rs from RouteStop rs
            join fetch rs.stop
            where rs.route.id = :routeId
              and rs.id.directionId = :directionId
            order by rs.id.sequenceNumber
            """)
    List<RouteStop> findRouteStops(@Param("routeId") Long routeId,
                                   @Param("directionId") short directionId);

    @Query("""
            select rs from RouteStop rs
            join fetch rs.route
            where rs.stop.id = :stopId
            """)
    List<RouteStop> findRoutesByStopId(@Param("stopId") Long stopId);
}
