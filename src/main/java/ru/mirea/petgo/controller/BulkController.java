package ru.mirea.petgo.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.petgo.dto.BulkRequestDto;
import ru.mirea.petgo.dto.BulkResponseDto;
import ru.mirea.petgo.service.BulkService;

@RestController
@RequestMapping("/api/walk-requests")
public class BulkController {

    private final BulkService bulkService;

    public BulkController(BulkService bulkService) {
        this.bulkService = bulkService;
    }

    /** Всегда 200: ошибки отдельных заявок описаны в results. 400 только для неверного запроса целиком. */
    @PostMapping("/bulk")
    public BulkResponseDto bulk(@Valid @RequestBody BulkRequestDto dto) {
        return bulkService.execute(dto);
    }
}
