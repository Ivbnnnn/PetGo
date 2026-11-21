package ru.mirea.petgo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.petgo.domain.*;
import ru.mirea.petgo.dto.WalkRequestCreateDto;
import ru.mirea.petgo.dto.WalkRequestFilterDto;
import ru.mirea.petgo.dto.WalkRequestResponseDto;
import ru.mirea.petgo.dto.WalkRequestUpdateDto;
import ru.mirea.petgo.filter.WalkRequestSpecification;
import ru.mirea.petgo.repository.*;
import ru.mirea.petgo.util.WalkRequestMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalkRequestService {

    private final WalkRequestRepository requestRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final WalkRequestMapper requestMapper;

    @Transactional
    public WalkRequestResponseDto create(WalkRequestCreateDto dto) {
        Pet pet = petRepository.findById(dto.getPetId()).orElseThrow(() -> new RuntimeException("Питомец не найден"));
        User owner = userRepository.findById(dto.getOwnerId()).orElseThrow(() -> new RuntimeException("Владелец не найден"));
        
        WalkRequest request = new WalkRequest();
        request.setExternalId("WR-" + UUID.randomUUID());
        request.setPet(pet);
        request.setOwner(owner);
        
        if (dto.getWalkerId() != null) {
            User walker = userRepository.findById(dto.getWalkerId()).orElseThrow(() -> new RuntimeException("Выгульщик не найден"));
            request.setWalker(walker);
        }

        request.setScheduledAt(dto.getScheduledAt());
        request.setDurationMinutes(dto.getDurationMinutes());
        request.setPriority(RequestPriority.forDuration(dto.getDurationMinutes()));
        request.setAddress(dto.getAddress());
        request.setDescription(dto.getDescription());
        request.setPrice(dto.getPrice());
        
        request.setStatus(RequestStatus.CREATED);
        request.setIsArchived(false);

        return requestMapper.toDto(requestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public Page<WalkRequestResponseDto> getAll(Pageable pageable) {
        return requestRepository.findAll(pageable).map(requestMapper::toDto);
    }

    @Transactional(readOnly = true)
    public WalkRequestResponseDto getById(UUID id) {
        WalkRequest request = requestRepository.findById(id).orElseThrow(() -> new RuntimeException("Заявка не найдена"));
        return requestMapper.toDto(request);
    }

    @Transactional
    public WalkRequestResponseDto update(UUID id, WalkRequestUpdateDto dto) {
        WalkRequest request = requestRepository.findById(id).orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        if (!request.getVersion().equals(dto.getVersion())) {
            throw new RuntimeException("CONFLICT");
        }

        if (dto.getWalkerId() != null) {
            User walker = userRepository.findById(dto.getWalkerId()).orElseThrow(() -> new RuntimeException("Выгульщик не найден"));
            request.setWalker(walker);
        }
        
        if (dto.getScheduledAt() != null) request.setScheduledAt(dto.getScheduledAt());
        if (dto.getDurationMinutes() != null) {
            request.setDurationMinutes(dto.getDurationMinutes());
            request.setPriority(RequestPriority.forDuration(dto.getDurationMinutes()));
        }
        if (dto.getAddress() != null) request.setAddress(dto.getAddress());
        if (dto.getDescription() != null) request.setDescription(dto.getDescription());
        if (dto.getPrice() != null) request.setPrice(dto.getPrice());

        return requestMapper.toDto(requestRepository.save(request));
    }

    @Transactional
    public WalkRequestResponseDto start(UUID id) {
        WalkRequest request = requestRepository.findById(id).orElseThrow(() -> new RuntimeException("Заявка не найдена"));
        if (request.getStatus() != RequestStatus.CREATED) {
            throw new RuntimeException("Начать можно только заявку в статусе CREATED");
        }
        request.setStatus(RequestStatus.IN_PROGRESS);
        return requestMapper.toDto(requestRepository.save(request));
    }

    @Transactional
    public WalkRequestResponseDto complete(UUID id) {
        WalkRequest request = requestRepository.findById(id).orElseThrow(() -> new RuntimeException("Заявка не найдена"));
        if (request.getStatus() != RequestStatus.IN_PROGRESS) {
            throw new RuntimeException("Завершить можно только заявку в статусе IN_PROGRESS");
        }
        request.setStatus(RequestStatus.COMPLETED);
        return requestMapper.toDto(requestRepository.save(request));
    }

    @Transactional
    public WalkRequestResponseDto cancel(UUID id) {
        WalkRequest request = requestRepository.findById(id).orElseThrow(() -> new RuntimeException("Заявка не найдена"));
        if (request.getStatus() == RequestStatus.COMPLETED || request.getStatus() == RequestStatus.CANCELLED) {
            throw new RuntimeException("Нельзя отменить завершенную или отмененную заявку");
        }
        request.setStatus(RequestStatus.CANCELLED);
        return requestMapper.toDto(requestRepository.save(request));
    }

    @Transactional
    public void archive(UUID id) {
        WalkRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));
        request.setIsArchived(true);
        requestRepository.save(request);
    }

    @Transactional
    public void restore(UUID id) {
        WalkRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));
        request.setIsArchived(false);
        requestRepository.save(request);
    }

    @Transactional
    public void permanentDelete(UUID id) {
        WalkRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));
        requestRepository.delete(request);
    }

    @Transactional(readOnly = true)
    public Page<WalkRequestResponseDto> getAll(WalkRequestFilterDto filter, Pageable pageable) {
        Specification<WalkRequest> spec = WalkRequestSpecification.getFiltered(filter);
        return requestRepository.findAll(spec, pageable).map(requestMapper::toDto);
    }
}
