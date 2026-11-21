package ru.mirea.petgo.domain;

public enum RequestPriority {
    LOW, NORMAL, HIGH;

    public static RequestPriority forDuration(int minutes) {
        if (minutes >= 60) {
            return HIGH;
        }
        return minutes >= 30 ? NORMAL : LOW;
    }
}
