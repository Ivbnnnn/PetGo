package ru.mirea.petgo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.petgo.domain.AccessLevel;
import ru.mirea.petgo.domain.AuditEvent.EntityType;
import ru.mirea.petgo.domain.Pet;
import ru.mirea.petgo.domain.User;
import ru.mirea.petgo.domain.UserRole;
import ru.mirea.petgo.dto.PetDto;
import ru.mirea.petgo.error.BadRequestException;
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
    public Page<PetDto> list(UUID authenticatedUserId, UUID ownerId, Pageable page) {
        User currentUser = requireActiveUser(authenticatedUserId);
        Pageable sorted = PageSort.apply(page, Sort.by("name"), SORT_FIELDS);
        UUID visibleOwner = canSeeAll(currentUser) ? ownerId : authenticatedUserId;
        Page<Pet> result = visibleOwner == null
                ? pets.findAll(sorted) : pets.findAllByOwner_Id(visibleOwner, sorted);
        return result.map(PetDto::from);
    }

    @Transactional(readOnly = true)
    public PetDto get(UUID id, UUID authenticatedUserId) {
        User currentUser = requireActiveUser(authenticatedUserId);

        Pet pet = canSeeAll(currentUser)
                ? find(id)
                : findOwned(id, authenticatedUserId);
        return PetDto.from(pet);
    }

    @Transactional
    public PetDto create(UUID authenticatedUserId, PetDto.Write input) {
        User currentUser = requireActiveUser(authenticatedUserId);
        UUID ownerId = input.ownerId();
        User owner;
        if (isAdmin(currentUser) && ownerId != null) {
            owner = requireOwner(requireActiveUser(ownerId));
        } else {
            if (isAdmin(currentUser) && currentUser.getRole() != UserRole.OWNER) {
                throw new BadRequestException("Укажите владельца питомца");
            }
            rejectOwnerSubstitution(ownerId, authenticatedUserId);
            owner = requireOwner(currentUser);
        }
        Pet pet = new Pet();
        pet.setOwner(owner);
        updateDetails(pet, input);
        pets.save(pet);
        audit.record("PET_CREATED", EntityType.PET, pet.getId(), "Создан питомец");
        return PetDto.from(pet);
    }

    @Transactional
    public PetDto update(UUID id, UUID authenticatedUserId, PetDto.Write input) {
        User currentUser = requireActiveUser(authenticatedUserId);
        Pet pet = findEditable(id, currentUser);
        UUID ownerId = input.ownerId();
        if (isAdmin(currentUser)) {
            if (ownerId != null && !ownerId.equals(pet.getOwner().getId())) {
                if (requests.existsByPet_Id(id)) {
                    throw new ConflictException("Нельзя сменить владельца питомца, у которого есть заявки на выгул");
                }
                pet.setOwner(requireOwner(requireActiveUser(ownerId)));
            }
        } else {
            rejectOwnerSubstitution(ownerId, authenticatedUserId);
        }
        updateDetails(pet, input);
        audit.record("PET_UPDATED", EntityType.PET, id, "Изменены данные питомца");
        return PetDto.from(pet);
    }

    @Transactional
    public void delete(UUID id, UUID authenticatedUserId) {
        Pet pet = findEditable(id, requireActiveUser(authenticatedUserId));
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

    private Pet find(UUID id) {
        return pets.findById(id).orElseThrow(() -> new ResourceNotFoundException("Питомец не найден"));
    }

    private Pet findEditable(UUID id, User currentUser) {
        return isAdmin(currentUser) ? find(id) : findOwned(id, requireOwner(currentUser).getId());
    }

    private boolean canSeeAll(User user) {
        return isAdmin(user) || user.getAccessLevel() == AccessLevel.EDITOR;
    }

    private boolean isAdmin(User user) {
        return user.getAccessLevel() == AccessLevel.ADMIN;
    }

    private User requireActiveUser(UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (!user.isEnabled()) {
            throw new AccessDeniedException("Учётная запись отключена");
        }
        return user;
    }

    private User requireOwner(User user) {
        if (user.getRole() != UserRole.OWNER) {
            throw new AccessDeniedException("Питомцу требуется учётная запись владельца");
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
