package ru.mirea.petgo.security;

import ru.mirea.petgo.error.BadRequestException;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {
    private PasswordPolicy() {
    }

    public static boolean fitsBcrypt(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length <= 72 && password.indexOf(0) < 0;
    }

    public static void validate(String password) {
        if (password.isBlank() || password.codePointCount(0, password.length()) < 12 || !fitsBcrypt(password)) {
            throw new BadRequestException("Пароль должен содержать от 12 символов до 72 байт UTF-8");
        }
    }
}
