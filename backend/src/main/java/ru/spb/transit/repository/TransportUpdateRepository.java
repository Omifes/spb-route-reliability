package ru.spb.transit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.spb.transit.domain.TransportUpdate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransportUpdateRepository extends JpaRepository<TransportUpdate, Long> {

    List<TransportUpdate> findByRouteExternalIdAndObservedAtAfterOrderByObservedAtDesc(
            String routeExternalId, Instant observedAfter);

    Optional<TransportUpdate> findFirstByRouteExternalIdOrderByObservedAtDesc(String routeExternalId);

    Optional<TransportUpdate> findFirstByRouteExternalIdAndStopExternalIdOrderByObservedAtDesc(
            String routeExternalId, String stopExternalId);

    @Modifying
    @Transactional
    @Query("delete from TransportUpdate t where t.observedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
