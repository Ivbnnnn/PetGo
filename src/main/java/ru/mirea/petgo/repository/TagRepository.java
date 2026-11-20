package ru.mirea.petgo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.petgo.domain.Tag;

import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    Optional<Tag> findByNameIgnoreCase(String name);
}
