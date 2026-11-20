package ru.mirea.petgo.error;

import java.time.Instant;

/** Единый JSON ошибки: без stack trace и внутренних сведений. */
public record ApiError(Instant timestamp, int status, String error, String message) {
}
