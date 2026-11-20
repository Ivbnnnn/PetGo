package ru.mirea.petgo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.petgo.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);
}
