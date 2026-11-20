package ru.mirea.petgo.error;

/** Некорректный запрос (HTTP 400). */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
