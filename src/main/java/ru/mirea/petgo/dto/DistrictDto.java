package ru.mirea.petgo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ru.mirea.petgo.domain.District;

import java.util.UUID;

public record DistrictDto(UUID id, String name, boolean enabled) {
    public record Write(@NotBlank @Size(max = 100) String name) {
    }

    public static DistrictDto from(District district) {
        return new DistrictDto(district.getId(), district.getName(), district.isEnabled());
    }
}
