package ru.mirea.petgo.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mirea.petgo.dto.PetDto;
import ru.mirea.petgo.service.PetService;

import java.util.UUID;

@RestController
@RequestMapping("/api/pets")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class PetController {
    private final PetService pets;

    @GetMapping
    public Page<PetDto> list(Pageable page) {
        return pets.list(page);
    }

    @GetMapping("/{id}")
    public PetDto get(@PathVariable UUID id) {
        return pets.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PetDto create(@Valid @RequestBody PetDto.Write input) {
        return pets.create(input);
    }

    @PutMapping("/{id}")
    public PetDto update(@PathVariable UUID id, @Valid @RequestBody PetDto.Write input) {
        return pets.update(id, input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        pets.delete(id);
    }
}
