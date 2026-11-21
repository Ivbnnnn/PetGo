package ru.mirea.petgo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.petgo.domain.*;
import ru.mirea.petgo.domain.AuditEvent.EntityType;
import ru.mirea.petgo.dto.WalkRequestCreateDto;
import ru.mirea.petgo.dto.WalkRequestFilterDto;
import ru.mirea.petgo.dto.WalkRequestResponseDto;
import ru.mirea.petgo.dto.WalkRequestUpdateDto;
import ru.mirea.petgo.error.ConflictException;
import ru.mirea.petgo.error.ResourceNotFoundException;
import ru.mirea.petgo.filter.WalkRequestSpecification;
import ru.mirea.petgo.filter.PageSort;
import ru.mirea.petgo.repository.*;
import ru.mirea.petgo.util.WalkRequestMapper;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalkRequestService {
    private static final Set<String> SORT_FIELDS = Set.of("id", "externalId", "scheduledAt", "durationMinutes",
            "address", "price", "status", "priority", "isArchived", "createdAt", "updatedAt");

    private final WalkRequestRepository requestRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final WalkRequestMapper requestMapper;
    private final DistrictService districts;
    private final AuditService audit;

    @Transactional
    public WalkRequestResponseDto create(WalkRequestCreateDto dto) {
        Pet pet = petRepository.findById(dto.getPetId())
                .orElseThrow(() -> new ResourceNotFoundException("Питомец не найден"));
        User owner = userRepository.findById(dto.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Владелец не найден"));
        if (!owner.getId().equals(pet.getOwner().getId())) {
            throw new ConflictException("Питомец принадлежит другому владельцу");
        }
        
        WalkRequest request = new WalkRequest();
        request.setExternalId("WR-" + UUID.randomUUID());
        request.setPet(pet);
        request.setOwner(owner);
        assignDistrict(request, dto.getDistrictId());
        
        if (dto.getWalkerId() != null) {
            request.setWalker(findWalker(dto.getWalkerId()));
        }

        request.setScheduledAt(dto.getScheduledAt());
        request.setDurationMinutes(dto.getDurationMinutes());
        request.setPriority(RequestPriority.forDuration(dto.getDurationMinutes()));
        request.setAddress(dto.getAddress());
        request.setDescription(dto.getDescription());
        request.setPrice(dto.getPrice());
        
        request.setStatus(RequestStatus.CREATED);
        request.setIsArchived(false);

        requestRepository.saveAndFlush(request);
        audit.record("REQUEST_CREATED", EntityType.WALK_REQUEST, request.getId(), "Создана заявка");
        return requestMapper.toDto(request);
    }

    @Transactional(readOnly = true)
    public WalkRequestResponseDto getById(UUID id) {
        return requestMapper.toDto(findRequest(id));
    }

    @Transactional
    public WalkRequestResponseDto update(UUID id, WalkRequestUpdateDto dto) {
        WalkRequest request = findRequest(id);

        if (!request.getVersion().equals(dto.getVersion())) {
            throw new ConflictException("Заявка уже изменена");
        }
        assignDistrict(request, dto.getDistrictId());

        if (dto.getWalkerId() != null) {
            request.setWalker(findWalker(dto.getWalkerId()));
        }
        
        if (dto.getScheduledAt() != null) request.setScheduledAt(dto.getScheduledAt());
        if (dto.getDurationMinutes() != null) {
            request.setDurationMinutes(dto.getDurationMinutes());
            request.setPriority(RequestPriority.forDuration(dto.getDurationMinutes()));
        }
        if (dto.getAddress() != null) request.setAddress(dto.getAddress());
        if (dto.getDescription() != null) request.setDescription(dto.getDescription());
        if (dto.getPrice() != null) request.setPrice(dto.getPrice());

        requestRepository.saveAndFlush(request);
        audit.record("REQUEST_UPDATED", EntityType.WALK_REQUEST, id, "Изменены данные заявки");
        return requestMapper.toDto(request);
    }

    @Transactional
    public WalkRequestResponseDto start(UUID id) {
        WalkRequest request = findActiveRequest(id);
        if (request.getStatus() != RequestStatus.CREATED) {
            throw new ConflictException("Начать можно только заявку в статусе CREATED");
        }
        return changeStatus(request, RequestStatus.IN_PROGRESS);
    }

    @Transactional
    public WalkRequestResponseDto complete(UUID id) {
        WalkRequest request = findActiveRequest(id);
        if (request.getStatus() != RequestStatus.IN_PROGRESS) {
            throw new ConflictException("Завершить можно только заявку в статусе IN_PROGRESS");
        }
        return changeStatus(request, RequestStatus.COMPLETED);
    }

    @Transactional
    public WalkRequestResponseDto cancel(UUID id) {
        WalkRequest request = findActiveRequest(id);
        if (request.getStatus() == RequestStatus.COMPLETED || request.getStatus() == RequestStatus.CANCELLED) {
            throw new ConflictException("Нельзя отменить завершённую или отменённую заявку");
        }
        return changeStatus(request, RequestStatus.CANCELLED);
    }

    @Transactional
    public void archive(UUID id) {
        WalkRequest request = findRequest(id);
        if (!Boolean.TRUE.equals(request.getIsArchived())) {
            request.setIsArchived(true);
            audit.record("REQUEST_ARCHIVED", EntityType.WALK_REQUEST, id, "Заявка архивирована");
        }
    }

    @Transactional
    public void restore(UUID id) {
        WalkRequest request = findRequest(id);
        if (Boolean.TRUE.equals(request.getIsArchived())) {
            request.setIsArchived(false);
            audit.record("REQUEST_RESTORED", EntityType.WALK_REQUEST, id, "Заявка восстановлена");
        }
    }

    @Transactional
    public void permanentDelete(UUID id) {
        requestRepository.delete(findRequest(id));
        audit.record("REQUEST_DELETED", EntityType.WALK_REQUEST, id, "Заявка удалена");
    }

    @Transactional(readOnly = true)
    public Page<WalkRequestResponseDto> getAll(WalkRequestFilterDto filter, Pageable pageable) {
        Specification<WalkRequest> spec = WalkRequestSpecification.getFiltered(filter);
        return requestRepository.findAll(spec, PageSort.apply(pageable, Sort.by("scheduledAt"), SORT_FIELDS))
                .map(requestMapper::toDto);
    }

    private WalkRequest findRequest(UUID id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));
    }

    private WalkRequestResponseDto changeStatus(WalkRequest request, RequestStatus target) {
        request.setStatus(target);
        requestRepository.saveAndFlush(request);
        audit.record("REQUEST_STATUS_CHANGED", EntityType.WALK_REQUEST, request.getId(), "Новый статус: " + target);
        return requestMapper.toDto(request);
    }

    private WalkRequest findActiveRequest(UUID id) {
        WalkRequest request = findRequest(id);
        if (Boolean.TRUE.equals(request.getIsArchived())) {
            throw new ConflictException("Заявка в архиве");
        }
        return request;
    }

    private User findWalker(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Выгульщик не найден"));
    }

    private void assignDistrict(WalkRequest request, UUID id) {
        if (id != null && (request.getDistrict() == null || !id.equals(request.getDistrict().getId()))) {
            request.setDistrict(districts.assignable(id));
        }
    }
}
