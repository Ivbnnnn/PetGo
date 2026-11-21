package ru.mirea.petgo.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import ru.mirea.petgo.domain.Pet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PetDto(UUID id, String name, UUID ownerId, String breed, BigDecimal weight, Integer age,
                     String specialNeeds, String photoUrl, LocalDateTime createdAt) {
    public record Write(@NotBlank @Size(max = 100) String name,
                        @NotNull UUID ownerId,
                        @Size(max = 100) String breed,
                        @Positive @Digits(integer = 3, fraction = 2) BigDecimal weight,
                        @PositiveOrZero Integer age,
                        @Size(max = 2000) String specialNeeds,
                        @Size(max = 255) String photoUrl) {
    }

    public static PetDto from(Pet pet) {
        return new PetDto(pet.getId(), pet.getName(), pet.getOwner().getId(), pet.getBreed(), pet.getWeight(),
                pet.getAge(), pet.getSpecialNeeds(), pet.getPhotoUrl(), pet.getCreatedAt());
    }
}
