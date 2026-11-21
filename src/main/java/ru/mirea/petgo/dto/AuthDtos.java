package ru.mirea.petgo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record Login(@NotBlank @Size(max = 254) String email,
                        @NotBlank @Size(max = 128) String password) {
        @Override
        public String toString() {
            return "Login[credentials redacted]";
        }
    }

    public record Refresh(@NotBlank @Size(max = 128) String refreshToken) {
        @Override
        public String toString() {
            return "Refresh[token redacted]";
        }
    }

    public record ChangePassword(@NotBlank @Size(max = 128) String currentPassword,
                                 @NotBlank @Size(max = 128) String newPassword) {
        @Override
        public String toString() {
            return "ChangePassword[credentials redacted]";
        }
    }

    public record Tokens(String accessToken, Instant accessExpiresAt, String refreshToken,
                         Instant refreshExpiresAt, UserDto user) {
        @Override
        public String toString() {
            return "Tokens[tokens redacted]";
        }
    }
}
