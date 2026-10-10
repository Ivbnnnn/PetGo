package ru.mirea.petgo.dto;

public record WalkRequestOption(int requestId, String label) {
    @Override
    public String toString() {
        return label;
    }
}