package ru.spb.transit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.spb.transit.domain.Stop;

import java.util.List;
import java.util.Optional;

public interface StopRepository extends JpaRepository<Stop, Long> {

    Optional<Stop> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    @Query("""
            select s from Stop s
            where s.name ilike concat('%', :name, '%')
            order by s.name
            """)
    List<Stop> searchByName(@Param("name") String name);
}
