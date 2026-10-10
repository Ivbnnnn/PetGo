package ru.mirea.petgo.util;

import java.math.BigDecimal;

import ru.mirea.petgo.exception.ValidationException;

public class Validators {
    private Validators() {
    }

    public static String requireText(String field, String label, String value, int max) throws ValidationException {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field, label + ": обязательное поле");
        }
        String v = value.trim();
        if (v.length() > max) {
            throw new ValidationException(field, label + ": не больше " + max + " символов");
        }
        return v;
    }

    public static void optionalText(String field, String label, String value, int max) throws ValidationException {
        if (value != null && value.trim().length() > max) {
            throw new ValidationException(field, label + ": не больше " + max + " символов");
        }
    }

    public static void email(String field, String value) throws ValidationException {
        if (!value.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$"))
            throw new ValidationException(field, "Email: неверный формат (пример: name@mail.ru)");
    }

    public static void optionalPhone(String field, String value) throws ValidationException {
        if (value != null && !value.isBlank() && !value.matches("^\\+?[0-9\\s()-]{7,20}$"))
            throw new ValidationException(field, "Телефон: неверный формат (пример: +79161234567)");
    }

    public static void range(String field, String label, BigDecimal value,
            BigDecimal min, BigDecimal max) throws ValidationException {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0)
            throw new ValidationException(field, label + ": значение от " + min + " до " + max);
    }
}
