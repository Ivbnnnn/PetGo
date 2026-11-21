package ru.mirea.petgo.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
    public Page<PetDto> list(@AuthenticationPrincipal Jwt jwt, Pageable page) {
        return pets.list(currentUserId(jwt), page);
    }

    @GetMapping("/{id}")
    public PetDto get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return pets.get(id, currentUserId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PetDto create(@Valid @RequestBody PetDto.Write input, @AuthenticationPrincipal Jwt jwt) {
        return pets.create(currentUserId(jwt), input);
    }

    @PutMapping("/{id}")
    public PetDto update(@PathVariable UUID id, @Valid @RequestBody PetDto.Write input,
                         @AuthenticationPrincipal Jwt jwt) {
        return pets.update(id, currentUserId(jwt), input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        pets.delete(id, currentUserId(jwt));
    }

    private static UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
