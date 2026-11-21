
package ru.mirea.petgo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.mirea.petgo.domain.WalkHistory;

import java.util.Optional;
import java.util.UUID;

public interface WalkHistoryRepository extends
        JpaRepository<WalkHistory, UUID>,
        JpaSpecificationExecutor<WalkHistory> {

    Optional<WalkHistory> findByWalkRequest_Id(UUID requestId);

    boolean existsByWalkRequest_Id(UUID requestId);
}
