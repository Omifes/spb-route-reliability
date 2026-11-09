package ru.spb.transit.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.spb.transit.domain.SavedTrip;

import java.util.List;
import java.util.Optional;

public interface SavedTripRepository extends JpaRepository<SavedTrip, Long> {

    @EntityGraph(attributePaths = {"route", "fromStop", "toStop"})
    List<SavedTrip> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"route", "fromStop", "toStop"})
    Optional<SavedTrip> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
}
