package ru.mirea.petgo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import ru.mirea.petgo.domain.AccessLevel;
import ru.mirea.petgo.domain.UserRole;

public final class UserAdminDtos {
    private UserAdminDtos() {
    }

    public record Create(@NotBlank @Size(max = 160) String username,
                         @NotBlank @Email @Size(max = 254) String email,
                         @NotNull UserRole role, @NotNull AccessLevel accessLevel,
                         @NotBlank @Size(max = 128) String temporaryPassword) {
        @Override
        public String toString() {
            return "Create[credentials redacted]";
        }
    }

    public record Update(@NotBlank @Size(max = 160) String username,
                         @NotBlank @Email @Size(max = 254) String email,
                         @NotNull UserRole role, @NotNull AccessLevel accessLevel,
                         @NotNull Boolean enabled, @NotNull @PositiveOrZero Long version) {
    }

    public record ResetPassword(@NotBlank @Size(max = 128) String temporaryPassword) {
        @Override
        public String toString() {
            return "ResetPassword[credentials redacted]";
        }
    }
}
