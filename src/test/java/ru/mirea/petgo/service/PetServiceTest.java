package ru.mirea.petgo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import ru.mirea.petgo.domain.AccessLevel;
import ru.mirea.petgo.domain.Pet;
import ru.mirea.petgo.domain.User;
import ru.mirea.petgo.domain.UserRole;
import ru.mirea.petgo.dto.PetDto;
import ru.mirea.petgo.error.ResourceNotFoundException;
import ru.mirea.petgo.repository.PetRepository;
import ru.mirea.petgo.repository.UserRepository;
import ru.mirea.petgo.repository.WalkRequestRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {
    @Mock PetRepository pets;
    @Mock UserRepository users;
    @Mock WalkRequestRepository requests;
    @Mock AuditService audit;
    @InjectMocks PetService service;

    private UUID ownerId;
    private UUID otherOwnerId;
    private User owner;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        otherOwnerId = UUID.randomUUID();
        owner = new User();
        owner.setId(ownerId);
        owner.setRole(UserRole.OWNER);
        owner.setEnabled(true);
        when(users.findById(ownerId)).thenReturn(Optional.of(owner));
    }

    @Test
    void listReturnsOnlyAuthenticatedOwnersPets() {
        when(pets.findAllByOwner_Id(eq(ownerId), any())).thenReturn(new PageImpl<>(List.of()));

        service.list(ownerId, otherOwnerId, PageRequest.of(0, 20));

        verify(pets).findAllByOwner_Id(eq(ownerId), any());
        verify(pets, never()).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void createUsesAuthenticatedOwnerAndRejectsOwnerSubstitution() {
        PetDto.Write valid = write(null);
        when(pets.save(any(Pet.class))).thenAnswer(invocation -> {
            Pet saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        PetDto result = service.create(ownerId, valid);

        assertEquals(ownerId, result.ownerId());
        verify(pets).save(argThat(pet -> pet.getOwner().getId().equals(ownerId)));
        assertThrows(AccessDeniedException.class, () -> service.create(ownerId, write(otherOwnerId)));
        verify(pets, times(1)).save(any(Pet.class));
    }

    @Test
    void ownerCanReadOwnPet() {
        UUID petId = UUID.randomUUID();
        Pet pet = new Pet();
        pet.setId(petId);
        pet.setName("Бобик");
        pet.setOwner(owner);
        when(pets.findByIdAndOwner_Id(petId, ownerId)).thenReturn(Optional.of(pet));

        PetDto result = service.get(petId, ownerId);

        assertEquals(petId, result.id());
        assertEquals(ownerId, result.ownerId());
    }

    @Test
    void ownerCanUpdateOwnPetButCannotTransferIt() {
        UUID petId = UUID.randomUUID();
        Pet pet = new Pet();
        pet.setId(petId);
        pet.setName("Старое имя");
        pet.setOwner(owner);
        when(pets.findByIdAndOwner_Id(petId, ownerId)).thenReturn(Optional.of(pet));

        PetDto result = service.update(petId, ownerId, write(null));

        assertEquals("Бобик", result.name());
        assertEquals(ownerId, result.ownerId());
        assertThrows(AccessDeniedException.class,
                () -> service.update(petId, ownerId, write(otherOwnerId)));
    }

    @Test
    void ownerCanDeleteOwnPetWithoutWalkRequests() {
        UUID petId = UUID.randomUUID();
        Pet pet = new Pet();
        pet.setId(petId);
        pet.setOwner(owner);
        when(pets.findByIdAndOwner_Id(petId, ownerId)).thenReturn(Optional.of(pet));
        when(requests.existsByPet_Id(petId)).thenReturn(false);

        assertDoesNotThrow(() -> service.delete(petId, ownerId));

        verify(pets).delete(pet);
    }

    @Test
    void ownerCannotReadAnotherOwnersPet() {
        UUID petId = UUID.randomUUID();
        when(pets.findByIdAndOwner_Id(petId, ownerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.get(petId, ownerId));
        verify(pets).findByIdAndOwner_Id(petId, ownerId);
    }

    @Test
    void ownerCannotUpdateAnotherOwnersPet() {
        UUID petId = UUID.randomUUID();
        when(pets.findByIdAndOwner_Id(petId, ownerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(petId, ownerId, write(null)));
        verify(pets, never()).save(any(Pet.class));
    }

    @Test
    void ownerCannotDeleteAnotherOwnersPet() {
        UUID petId = UUID.randomUUID();
        when(pets.findByIdAndOwner_Id(petId, ownerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(petId, ownerId));
        verify(pets, never()).delete(any(Pet.class));
    }

    @Test
    void walkerCanReadPetsButCannotCreateThem() {
        User walker = new User();
        walker.setId(ownerId);
        walker.setRole(UserRole.WALKER);
        walker.setAccessLevel(AccessLevel.EDITOR);
        walker.setEnabled(true);
        when(users.findById(ownerId)).thenReturn(Optional.of(walker));
        when(pets.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertDoesNotThrow(() -> service.list(ownerId, null, PageRequest.of(0, 20)));
        assertThrows(AccessDeniedException.class, () -> service.create(ownerId, write(null)));
        verify(pets).findAll(any(org.springframework.data.domain.Pageable.class));
        verify(pets, never()).save(any(Pet.class));
    }

    @Test
    void administratorCanManageAnotherOwnersPetRegardlessOfDomainRole() {
        UUID adminId = UUID.randomUUID();
        User admin = new User();
        admin.setId(adminId);
        admin.setRole(UserRole.WALKER);
        admin.setAccessLevel(AccessLevel.ADMIN);
        when(users.findById(adminId)).thenReturn(Optional.of(admin));
        when(pets.save(any(Pet.class))).thenAnswer(invocation -> {
            Pet saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            when(pets.findById(saved.getId())).thenReturn(Optional.of(saved));
            return saved;
        });

        PetDto created = service.create(adminId, write(ownerId));
        PetDto updated = service.update(created.id(), adminId, write(null));
        service.delete(created.id(), adminId);

        assertEquals(ownerId, created.ownerId());
        assertEquals(ownerId, updated.ownerId());
        verify(pets).delete(argThat(pet -> pet.getId().equals(created.id())));
    }

    @Test
    void cannotDeletePetWithWalkRequests() {
        UUID petId = UUID.randomUUID();
        Pet pet = new Pet();
        pet.setId(petId);
        pet.setOwner(owner);
        when(pets.findByIdAndOwner_Id(petId, ownerId)).thenReturn(Optional.of(pet));
        when(requests.existsByPet_Id(petId)).thenReturn(true);

        assertThrows(ru.mirea.petgo.error.ConflictException.class, () -> service.delete(petId, ownerId));
        verify(pets, never()).delete(any(Pet.class));
    }

    private static PetDto.Write write(UUID ownerId) {
        return new PetDto.Write("Бобик", ownerId, "Метис", new BigDecimal("12.50"), 4,
                null, null);
    }
}
