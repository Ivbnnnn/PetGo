package ru.mirea.petgo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mirea.petgo.dto.WalkRequestCreateDto;
import ru.mirea.petgo.dto.WalkRequestFilterDto;
import ru.mirea.petgo.dto.WalkRequestResponseDto;
import ru.mirea.petgo.dto.WalkRequestUpdateDto;
import ru.mirea.petgo.service.WalkRequestService;

import java.util.UUID;

@RestController
@RequestMapping("/api/walk-requests")
@RequiredArgsConstructor
public class WalkRequestController {

    private final WalkRequestService requestService;

    @GetMapping("/{id}")
    public WalkRequestResponseDto getById(@PathVariable UUID id) {
        return requestService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WalkRequestResponseDto create(@Valid @RequestBody WalkRequestCreateDto dto) {
        return requestService.create(dto);
    }

    @PutMapping("/{id}")
    public WalkRequestResponseDto update(@PathVariable UUID id, @Valid @RequestBody WalkRequestUpdateDto dto) {
        return requestService.update(id, dto);
    }

    @PostMapping("/{id}/start")
    public WalkRequestResponseDto start(@PathVariable UUID id) {
        return requestService.start(id);
    }

    @PostMapping("/{id}/complete")
    public WalkRequestResponseDto complete(@PathVariable UUID id) {
        return requestService.complete(id);
    }

    @PostMapping("/{id}/cancel")
    public WalkRequestResponseDto cancel(@PathVariable UUID id) {
        return requestService.cancel(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id) {
        requestService.archive(id);
    }

    @PostMapping("/{id}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restore(@PathVariable UUID id) {
        requestService.restore(id);
    }

    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void permanentDelete(@PathVariable UUID id) {
        requestService.permanentDelete(id);
    }

    @GetMapping
    public Page<WalkRequestResponseDto> getAll(
            @ModelAttribute WalkRequestFilterDto filter,
            Pageable pageable) {
        return requestService.getAll(filter, pageable);
    }
}
