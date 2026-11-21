
package ru.mirea.petgo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ru.mirea.petgo.dto.RequestNoteDto;
import ru.mirea.petgo.service.RequestNoteService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RequestNoteController {

    private final RequestNoteService service;

    @GetMapping("/api/walk-requests/{id}/notes")
    public List<RequestNoteDto> list(
            @PathVariable UUID id
    ) {
        return service.list(id);
    }

    @PostMapping("/api/walk-requests/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestNoteDto create(
            @PathVariable UUID id,
            @Valid @RequestBody RequestNoteDto dto
    ) {
        return service.create(id, dto);
    }

    @PutMapping("/api/notes/{noteId}")
    public RequestNoteDto update(
            @PathVariable UUID noteId,
            @Valid @RequestBody RequestNoteDto dto
    ) {
        return service.update(noteId, dto);
    }

    @DeleteMapping("/api/notes/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID noteId) {
        service.delete(noteId);
    }

    @PostMapping("/api/notes/{noteId}/pin")
    public RequestNoteDto pin(@PathVariable UUID noteId) {
        return service.pin(noteId, true);
    }

    @PostMapping("/api/notes/{noteId}/unpin")
    public RequestNoteDto unpin(@PathVariable UUID noteId) {
        return service.pin(noteId, false);
    }
}
