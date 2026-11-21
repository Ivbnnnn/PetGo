package ru.mirea.petgo.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class WalkRequestResponseDto {
    private UUID id;
    private String externalId;
    private UUID petId;
    private UUID ownerId;
    private UUID walkerId;
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private String address;
    private String description;
    private Double price;
    private String status;
    private String priority;
    private Boolean isArchived;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TagDto> tags;
}
