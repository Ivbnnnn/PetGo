package ru.mirea.petgo.filter;

import java.util.Locale;

public final class LikePattern {
    private LikePattern() {
    }

    public static String contains(String text) {
        return "%" + text.strip().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
