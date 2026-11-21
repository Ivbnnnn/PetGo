package ru.mirea.petgo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.petgo.domain.Pet;

import java.util.Optional;
import java.util.UUID;

public interface PetRepository extends JpaRepository<Pet, UUID> {

    Page<Pet> findAllByOwner_Id(UUID ownerId, Pageable pageable);

    Optional<Pet> findByIdAndOwner_Id(UUID id, UUID ownerId);

    Optional<Pet> findFirstByOwner_IdAndNameIgnoreCase(UUID ownerId, String name);
}
