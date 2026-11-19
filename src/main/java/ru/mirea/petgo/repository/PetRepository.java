package ru.mirea.petgo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.petgo.domain.Pet;
import java.util.UUID;

public interface PetRepository extends JpaRepository<Pet, UUID> {
}