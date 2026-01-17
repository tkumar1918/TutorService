package dev.tushar.tutorservice.repository;

import dev.tushar.tutorservice.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = { "roles.permissions", "permissions" })
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
