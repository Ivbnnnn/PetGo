package ru.mirea.petgo.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mirea.petgo.domain.RefreshSession;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshSession> findByTokenHash(String tokenHash);

    @EntityGraph(attributePaths = "user")
    Optional<RefreshSession> findByIdAndUser_Id(UUID id, UUID userId);

    @Modifying
    @Query("delete from RefreshSession s where s.expiresAt <= :now")
    void deleteExpired(@Param("now") Instant now);
}
