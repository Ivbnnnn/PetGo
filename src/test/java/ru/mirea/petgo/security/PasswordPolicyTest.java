package ru.mirea.petgo.security;

import org.junit.jupiter.api.Test;
import ru.mirea.petgo.error.BadRequestException;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {
    @Test
    void acceptsValidLengthIncludingTheBcryptLimit() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("a".repeat(12)));
        assertDoesNotThrow(() -> PasswordPolicy.validate("я".repeat(36)));
    }

    @Test
    void rejectsShortAndOversizedPasswords() {
        assertThrows(BadRequestException.class, () -> PasswordPolicy.validate("a".repeat(11)));
        assertThrows(BadRequestException.class, () -> PasswordPolicy.validate("я".repeat(37)));
    }
}
