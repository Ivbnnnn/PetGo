package ru.mirea.petgo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TagCreateDto(
        @NotBlank(message = "Название тега обязательно")
        @Size(max = 50, message = "Название не должно превышать 50 символов")
        String name,

        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Цвет в формате #RRGGBB")
        String color) {
}
