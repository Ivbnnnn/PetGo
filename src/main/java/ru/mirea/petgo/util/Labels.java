package ru.mirea.petgo.util;

import ru.mirea.petgo.model.enums.UserRole;
import ru.mirea.petgo.model.enums.WalkStatus;

public final class Labels {
    private Labels() {
    }

    public static String status(WalkStatus s) {
        if (s == null)
            return "—";
        return switch (s) {
            case CREATED -> "Создана";
            case IN_PROGRESS -> "В процессе";
            case COMPLETED -> "Завершена";
            case CANCELLED -> "Отменена";
        };
    }

    public static String role(UserRole r) {
        if (r == null)
            return "—";
        return switch (r) {
            case OWNER -> "Владелец";
            case WALKER -> "Выгульщик";
        };
    }
}