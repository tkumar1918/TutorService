package dev.tushar.tutorservice.repository;

import dev.tushar.tutorservice.entity.RefreshToken;
import dev.tushar.tutorservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    List<RefreshToken> findByUser(User user);

    Optional<RefreshToken> findByToken(String token);
}
