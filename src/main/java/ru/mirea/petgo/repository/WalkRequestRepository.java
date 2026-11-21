package ru.mirea.petgo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.mirea.petgo.domain.WalkRequest;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface WalkRequestRepository extends JpaRepository<WalkRequest, UUID>, JpaSpecificationExecutor<WalkRequest> {

    @Override
    @EntityGraph(attributePaths = { "pet", "owner", "walker" })
    Page<WalkRequest> findAll(Specification<WalkRequest> spec, Pageable pageable);

    List<WalkRequest> findByExternalIdIn(Collection<String> externalIds);

    boolean existsByPet_Id(UUID petId);
}
