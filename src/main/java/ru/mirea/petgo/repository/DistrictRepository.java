package ru.mirea.petgo.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.petgo.domain.District;

import java.util.List;
import java.util.UUID;

public interface DistrictRepository extends JpaRepository<District, UUID> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    List<District> findAllByEnabled(boolean enabled, Sort sort);
}
