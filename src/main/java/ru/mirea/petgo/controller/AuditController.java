package ru.mirea.petgo.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.petgo.domain.AuditEvent.EntityType;
import ru.mirea.petgo.dto.AuditDto;
import ru.mirea.petgo.service.AuditService;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/audit")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AuditController {
    private final AuditService audit;

    @GetMapping
    public Page<AuditDto> list(@RequestParam(defaultValue = "") String q,
                               @RequestParam(required = false) EntityType entityType,
                               @RequestParam(required = false) UUID actorId,
                               Pageable page) {
        return audit.search(q, entityType, actorId, page);
    }
}
