package ru.mirea.petgo.error;

/** Превышен допустимый размер файла (HTTP 413). */
public class PayloadTooLargeException extends RuntimeException {
    public PayloadTooLargeException(String message) {
        super(message);
    }
}
