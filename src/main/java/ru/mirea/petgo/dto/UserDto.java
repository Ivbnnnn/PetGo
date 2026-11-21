package ru.mirea.petgo.dto;

import ru.mirea.petgo.domain.AccessLevel;
import ru.mirea.petgo.domain.User;
import ru.mirea.petgo.domain.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserDto(UUID id, String username, String email, UserRole role, AccessLevel accessLevel,
                      boolean enabled, boolean passwordChangeRequired, Instant createdAt, long version) {
    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
                user.getAccessLevel(), user.isEnabled(), user.isPasswordChangeRequired(),
                user.getCreatedAt(), user.getVersion());
    }
}
