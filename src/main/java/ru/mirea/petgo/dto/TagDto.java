package ru.mirea.petgo.dto;

import ru.mirea.petgo.domain.Tag;

import java.util.UUID;

public record TagDto(UUID id, String name, String color) {

    public static TagDto from(Tag tag) {
        return new TagDto(tag.getId(), tag.getName(), tag.getColor());
    }
}
