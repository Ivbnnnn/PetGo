package ru.mirea.petgo.dto;

import lombok.Data;
import ru.mirea.petgo.domain.RequestStatus;
import java.time.LocalDateTime;

@Data
public class WalkRequestFilterDto {
    private String search;
    private RequestStatus status;
    private Boolean isArchived;
    private Double minPrice;
    private Double maxPrice;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
}