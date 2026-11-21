package ru.mirea.petgo.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import ru.mirea.petgo.domain.RequestPriority;
import ru.mirea.petgo.domain.RequestStatus;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class WalkRequestFilterDto {
    private String search;
    private String address;
    private RequestStatus status;
    private RequestPriority priority;
    private Boolean isArchived;
    private Boolean archived;
    private UUID tagId;
    private UUID districtId;
    private Double minPrice;
    private Double maxPrice;
    private Double priceFrom;
    private Double priceTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime scheduledFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime scheduledTo;
}
