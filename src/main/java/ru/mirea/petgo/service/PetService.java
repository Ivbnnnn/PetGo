package ru.mirea.petgo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
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
    public Page<PetDto> list(UUID authenticatedUserId, Pageable page) {
        User currentUser = requireActiveUser(authenticatedUserId);
        Pageable sorted = PageSort.apply(page, Sort.by("name"), SORT_FIELDS);
        Page<Pet> result = currentUser.getRole() == UserRole.OWNER
                ? pets.findAllByOwner_Id(authenticatedUserId, sorted)
                : pets.findAll(sorted);
        return result.map(PetDto::from);
    }

    @Transactional(readOnly = true)
    public PetDto get(UUID id, UUID authenticatedUserId) {
        User currentUser = requireActiveUser(authenticatedUserId);
        Pet pet = currentUser.getRole() == UserRole.OWNER
                ? findOwned(id, authenticatedUserId)
                : pets.findById(id).orElseThrow(() -> new ResourceNotFoundException("Питомец не найден"));
        return PetDto.from(pet);
    }

    @Transactional
    public PetDto create(UUID authenticatedUserId, PetDto.Write input) {
        User owner = requireOwner(authenticatedUserId);
        rejectOwnerSubstitution(input.ownerId(), authenticatedUserId);
        Pet pet = new Pet();
        pet.setOwner(owner);
        updateDetails(pet, input);
        pets.save(pet);
        audit.record("PET_CREATED", EntityType.PET, pet.getId(), "Создан питомец");
        return PetDto.from(pet);
    }

    @Transactional
    public PetDto update(UUID id, UUID authenticatedUserId, PetDto.Write input) {
        requireOwner(authenticatedUserId);
        rejectOwnerSubstitution(input.ownerId(), authenticatedUserId);
        Pet pet = findOwned(id, authenticatedUserId);
        updateDetails(pet, input);
        audit.record("PET_UPDATED", EntityType.PET, id, "Изменены данные питомца");
        return PetDto.from(pet);
    }

    @Transactional
    public void delete(UUID id, UUID authenticatedUserId) {
        requireOwner(authenticatedUserId);
        Pet pet = findOwned(id, authenticatedUserId);
        if (requests.existsByPet_Id(id)) {
            throw new ConflictException("Нельзя удалить питомца, у которого есть заявки на выгул");
        }
        pets.delete(pet);
        audit.record("PET_DELETED", EntityType.PET, id, "Питомец удалён");
    }

    private Pet findOwned(UUID id, UUID ownerId) {
        return pets.findByIdAndOwner_Id(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Питомец не найден"));
    }

    private User requireActiveUser(UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (!user.isEnabled()) {
            throw new AccessDeniedException("Учётная запись отключена");
        }
        return user;
    }

    private User requireOwner(UUID userId) {
        User user = requireActiveUser(userId);
        if (user.getRole() != UserRole.OWNER) {
            throw new AccessDeniedException("Управлять питомцами может только владелец");
        }
        return user;
    }

    private void rejectOwnerSubstitution(UUID requestedOwnerId, UUID authenticatedUserId) {
        if (requestedOwnerId != null && !requestedOwnerId.equals(authenticatedUserId)) {
            throw new AccessDeniedException("Нельзя назначить питомца другому владельцу");
        }
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
