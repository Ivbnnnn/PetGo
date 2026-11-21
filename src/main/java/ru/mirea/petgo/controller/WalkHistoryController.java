
package ru.mirea.petgo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ru.mirea.petgo.dto.WalkHistoryDto;
import ru.mirea.petgo.service.WalkHistoryService;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class WalkHistoryController {

    private final WalkHistoryService service;

    @GetMapping("/api/walk-history")
    public Page<WalkHistoryDto> list(
            @RequestParam(required = false) UUID requestId,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,
            Pageable pageable
    ) {
        return service.getAll(
                requestId, minRating, from, to, pageable
        );
    }

    @GetMapping("/api/walk-requests/{id}/history")
    public WalkHistoryDto byRequest(@PathVariable UUID id) {
        return service.getByRequestId(id);
    }

    @PostMapping("/api/walk-requests/{id}/history")
    public WalkHistoryDto create(
            @PathVariable UUID id,
            @Valid @RequestBody WalkHistoryDto dto
    ) {
        return service.createOrUpdate(id, dto);
    }

    @PutMapping("/api/walk-history/{id}")
    public WalkHistoryDto update(
            @PathVariable UUID id,
            @Valid @RequestBody WalkHistoryDto dto
    ) {
        return service.update(id, dto);
    }
}
