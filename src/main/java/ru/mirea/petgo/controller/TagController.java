package ru.mirea.petgo.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mirea.petgo.dto.TagCreateDto;
import ru.mirea.petgo.dto.TagDto;
import ru.mirea.petgo.service.TagService;

import java.util.List;
import java.util.UUID;

@RestController
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/api/tags")
    public List<TagDto> list() {
        return tagService.findAll();
    }

    @PostMapping("/api/tags")
    @ResponseStatus(HttpStatus.CREATED)
    public TagDto create(@Valid @RequestBody TagCreateDto dto) {
        return tagService.create(dto);
    }

    @PutMapping("/api/tags/{id}")
    public TagDto update(@PathVariable UUID id, @Valid @RequestBody TagCreateDto dto) {
        return tagService.update(id, dto);
    }

    @DeleteMapping("/api/tags/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        tagService.delete(id);
    }

    @PostMapping("/api/walk-requests/{id}/tags/{tagId}")
    public List<TagDto> attach(@PathVariable UUID id, @PathVariable UUID tagId) {
        return tagService.attach(id, tagId);
    }

    @DeleteMapping("/api/walk-requests/{id}/tags/{tagId}")
    public List<TagDto> detach(@PathVariable UUID id, @PathVariable UUID tagId) {
        return tagService.detach(id, tagId);
    }
}
