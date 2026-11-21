package ru.mirea.petgo.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mirea.petgo.dto.UserAdminDtos.Create;
import ru.mirea.petgo.dto.UserAdminDtos.ResetPassword;
import ru.mirea.petgo.dto.UserAdminDtos.Update;
import ru.mirea.petgo.dto.UserDto;
import ru.mirea.petgo.service.UserAdminService;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class UserAdminController {
    private final UserAdminService users;

    @GetMapping
    public Page<UserDto> list(Pageable page) {
        return users.list(page);
    }

    @GetMapping("/{id}")
    public UserDto get(@PathVariable UUID id) {
        return users.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody Create input) {
        return users.create(input);
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable UUID id, @Valid @RequestBody Update input) {
        return users.update(id, input);
    }

    @PostMapping("/{id}/block")
    public UserDto block(@PathVariable UUID id) {
        return users.setEnabled(id, false);
    }

    @PostMapping("/{id}/unblock")
    public UserDto unblock(@PathVariable UUID id) {
        return users.setEnabled(id, true);
    }

    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPassword input) {
        users.resetPassword(id, input.temporaryPassword());
    }
}
