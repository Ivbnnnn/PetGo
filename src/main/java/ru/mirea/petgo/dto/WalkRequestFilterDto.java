
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

    private UUID petId;
    private UUID ownerId;
    private UUID walkerId;
    private UUID districtId;
    private UUID tagId;

    private RequestStatus status;
    private RequestPriority priority;
    private Boolean isPriority;

    private Boolean isArchived;
    private Boolean archived;

    private Double minPrice;
    private Double maxPrice;
    private Double priceFrom;
    private Double priceTo;

    private String priceOperation;
    private Double priceValue;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime scheduledFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime scheduledTo;
}
