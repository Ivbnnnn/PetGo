package ru.mirea.petgo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.petgo.domain.AuditEvent.EntityType;
import ru.mirea.petgo.domain.Pet;
import ru.mirea.petgo.domain.User;
import ru.mirea.petgo.domain.UserRole;
import ru.mirea.petgo.dto.PetDto;
import ru.mirea.petgo.error.ConflictException;
import ru.mirea.petgo.error.ResourceNotFoundException;
import ru.mirea.petgo.filter.PageSort;
import ru.mirea.petgo.repository.PetRepository;
import ru.mirea.petgo.repository.UserRepository;
import ru.mirea.petgo.repository.WalkRequestRepository;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PetService {
    private static final Set<String> SORT_FIELDS = Set.of("id", "name", "breed", "age", "weight", "createdAt");
    private final PetRepository pets;
    private final UserRepository users;
    private final WalkRequestRepository requests;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public Page<PetDto> list(Pageable page) {
        return pets.findAll(PageSort.apply(page, Sort.by("name"), SORT_FIELDS)).map(PetDto::from);
    }

    @Transactional(readOnly = true)
    public PetDto get(UUID id) {
        return PetDto.from(find(id));
    }

    @Transactional
    public PetDto create(PetDto.Write input) {
        Pet pet = new Pet();
        pet.setOwner(findOwner(input.ownerId()));
        updateDetails(pet, input);
        pets.save(pet);
        audit.record("PET_CREATED", EntityType.PET, pet.getId(), "Создан питомец");
        return PetDto.from(pet);
    }

    @Transactional
    public PetDto update(UUID id, PetDto.Write input) {
        Pet pet = find(id);
        if (!input.ownerId().equals(pet.getOwner().getId())) {
            if (requests.existsByPet_Id(id)) {
                throw new ConflictException("У питомца с заявками нельзя изменить владельца");
            }
            pet.setOwner(findOwner(input.ownerId()));
        }
        updateDetails(pet, input);
        audit.record("PET_UPDATED", EntityType.PET, id, "Изменены данные питомца");
        return PetDto.from(pet);
    }

    @Transactional
    public void delete(UUID id) {
        pets.delete(find(id));
        audit.record("PET_DELETED", EntityType.PET, id, "Питомец удалён");
    }

    private Pet find(UUID id) {
        return pets.findById(id).orElseThrow(() -> new ResourceNotFoundException("Питомец не найден"));
    }

    private User findOwner(UUID id) {
        User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Владелец не найден"));
        if (user.getRole() != UserRole.OWNER || !user.isEnabled()) {
            throw new ConflictException("Укажите активного пользователя с ролью OWNER");
        }
        return user;
    }

    private void updateDetails(Pet pet, PetDto.Write input) {
        pet.setName(input.name().strip());
        pet.setBreed(input.breed());
        pet.setWeight(input.weight());
        pet.setAge(input.age());
        pet.setSpecialNeeds(input.specialNeeds());
        pet.setPhotoUrl(input.photoUrl());
    }
}
