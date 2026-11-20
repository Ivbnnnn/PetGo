package ru.mirea.petgo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.petgo.domain.Pet;

import java.util.Optional;
import java.util.UUID;

public interface PetRepository extends JpaRepository<Pet, UUID> {

    Optional<Pet> findFirstByOwner_IdAndNameIgnoreCase(UUID ownerId, String name);
}
