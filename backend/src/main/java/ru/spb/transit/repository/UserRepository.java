package ru.spb.transit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.spb.transit.domain.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
