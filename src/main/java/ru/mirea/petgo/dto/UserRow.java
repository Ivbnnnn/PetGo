package ru.mirea.petgo.dto;

import ru.mirea.petgo.model.enums.UserRole;

public record UserRow(
        int id,
        String name,
        String email,
        String phone,
        UserRole role,
        String address,
        boolean active) {
}