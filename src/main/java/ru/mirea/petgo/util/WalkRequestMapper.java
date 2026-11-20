package ru.mirea.petgo.util;

import org.springframework.stereotype.Component;
import ru.mirea.petgo.domain.WalkRequest;
import ru.mirea.petgo.dto.TagDto;
import ru.mirea.petgo.dto.WalkRequestResponseDto;

import java.util.Comparator;

@Component
public class WalkRequestMapper {

    public WalkRequestResponseDto toDto(WalkRequest entity) {
        if (entity == null) return null;

        WalkRequestResponseDto dto = new WalkRequestResponseDto();
        dto.setId(entity.getId());
        dto.setExternalId(entity.getExternalId());

        if (entity.getPet() != null) dto.setPetId(entity.getPet().getId());
        if (entity.getOwner() != null) dto.setOwnerId(entity.getOwner().getId());
        if (entity.getWalker() != null) dto.setWalkerId(entity.getWalker().getId());

        dto.setScheduledAt(entity.getScheduledAt());
        dto.setDurationMinutes(entity.getDurationMinutes());
        dto.setAddress(entity.getAddress());
        dto.setDescription(entity.getDescription());
        dto.setPrice(entity.getPrice());

        if (entity.getStatus() != null) dto.setStatus(entity.getStatus().name());
        if (entity.getPriority() != null) dto.setPriority(entity.getPriority().name());

        dto.setIsArchived(entity.getIsArchived());
        dto.setVersion(entity.getVersion());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        // теги читаются внутри транзакции сервиса (связь ленивая)
        dto.setTags(entity.getTags().stream()
                .map(TagDto::from)
                .sorted(Comparator.comparing(TagDto::name, String.CASE_INSENSITIVE_ORDER))
                .toList());

        return dto;
    }
}
