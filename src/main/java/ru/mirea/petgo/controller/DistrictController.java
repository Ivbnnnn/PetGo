package ru.mirea.petgo.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mirea.petgo.dto.DistrictDto;
import ru.mirea.petgo.service.DistrictService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/districts")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class DistrictController {
    private final DistrictService districts;

    @GetMapping
    public List<DistrictDto> list(@RequestParam(required = false) Boolean enabled) {
        return districts.list(enabled);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DistrictDto create(@RequestBody @Valid DistrictDto.Write input) {
        return districts.create(input.name());
    }

    @PutMapping("/{id}")
    public DistrictDto update(@PathVariable UUID id, @RequestBody @Valid DistrictDto.Write input) {
        return districts.update(id, input.name());
    }

    @PostMapping("/{id}/enable")
    public DistrictDto enable(@PathVariable UUID id) {
        return districts.setEnabled(id, true);
    }

    @PostMapping("/{id}/disable")
    public DistrictDto disable(@PathVariable UUID id) {
        return districts.setEnabled(id, false);
    }
}
