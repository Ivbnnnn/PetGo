package ru.mirea.petgo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.mirea.petgo.domain.WalkRequest;
import java.util.UUID;

public interface WalkRequestRepository extends JpaRepository<WalkRequest, UUID>, JpaSpecificationExecutor<WalkRequest> {
}