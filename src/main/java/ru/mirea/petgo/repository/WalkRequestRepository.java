package ru.mirea.petgo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.petgo.domain.WalkRequest;
import java.util.UUID;

public interface WalkRequestRepository extends JpaRepository<WalkRequest, UUID> {
}