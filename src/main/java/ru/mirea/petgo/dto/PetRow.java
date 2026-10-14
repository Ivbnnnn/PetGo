package ru.mirea.petgo.dto;

import java.math.BigDecimal;

public record PetRow(
        int id,
        String name,
        String breed,
        BigDecimal weight,
        Integer age,
        String specialNeeds,
        String photoUrl,
        int ownerId,
        String ownerName) {
}
