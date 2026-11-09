package ru.spb.transit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.spb.transit.domain.Route;
import ru.spb.transit.domain.RouteType;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {

    Optional<Route> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    List<Route> findByTypeOrderByNameAsc(RouteType type);
}
