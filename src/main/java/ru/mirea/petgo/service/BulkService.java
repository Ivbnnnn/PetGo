package ru.mirea.petgo.service;

import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import ru.mirea.petgo.domain.AuditEvent.EntityType;
import ru.mirea.petgo.domain.RequestStatus;
import ru.mirea.petgo.domain.WalkRequest;
import ru.mirea.petgo.dto.BulkAction;
import ru.mirea.petgo.dto.BulkItemResult;
import ru.mirea.petgo.dto.BulkRequestDto;
import ru.mirea.petgo.dto.BulkResponseDto;
import ru.mirea.petgo.error.BadRequestException;
import ru.mirea.petgo.error.ConflictException;
import ru.mirea.petgo.error.ResourceNotFoundException;
import ru.mirea.petgo.repository.TagRepository;
import ru.mirea.petgo.repository.WalkRequestRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * Массовые операции. Каждая заявка обрабатывается в своей транзакции,
 * поэтому ошибка одной заявки не откатывает остальные и описывается в отчёте.
 * Сам метод execute намеренно НЕ помечен @Transactional.
 */
@Service
public class BulkService {

    private static final Logger log = LoggerFactory.getLogger(BulkService.class);

    private final WalkRequestRepository requestRepository;
    private final TagRepository tagRepository;
    private final WalkRequestService requestService;
    private final PlatformTransactionManager txManager;
    private final AuditService audit;

    public BulkService(WalkRequestRepository requestRepository,
                       TagRepository tagRepository,
                       WalkRequestService requestService,
                       PlatformTransactionManager txManager,
                       AuditService audit) {
        this.requestRepository = requestRepository;
        this.tagRepository = tagRepository;
        this.requestService = requestService;
        this.txManager = txManager;
        this.audit = audit;
    }

    public BulkResponseDto execute(BulkRequestDto dto) {
        validate(dto);

        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        List<BulkItemResult> results = new ArrayList<>();
        for (UUID id : new LinkedHashSet<>(dto.ids())) {          // дубли в списке убираем
            try {
                tx.executeWithoutResult(status -> applyOne(id, dto));
                results.add(new BulkItemResult(id, true, null));
            } catch (Exception e) {
                results.add(new BulkItemResult(id, false, message(e)));
            }
        }
        int ok = (int) results.stream().filter(BulkItemResult::success).count();
        int failed = results.size() - ok;
        audit.record("BULK_OPERATION", EntityType.WALK_REQUEST, null,
                "%s: успешно %d, ошибок %d".formatted(dto.action(), ok, failed));
        return new BulkResponseDto(results.size(), ok, failed, results);
    }

    private void validate(BulkRequestDto dto) {
        if (dto.action() == BulkAction.CHANGE_STATUS && dto.status() == null) {
            throw new BadRequestException("Для смены статуса укажите status");
        }
        if (dto.action() == BulkAction.ADD_TAG || dto.action() == BulkAction.REMOVE_TAG) {
            if (dto.tagId() == null) {
                throw new BadRequestException("Укажите tagId");
            }
            if (!tagRepository.existsById(dto.tagId())) {
                throw new ResourceNotFoundException("Тег не найден");
            }
        }
    }

    private void applyOne(UUID id, BulkRequestDto dto) {
        WalkRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));

        switch (dto.action()) {
            case ARCHIVE -> {
                if (Boolean.TRUE.equals(request.getIsArchived())) {
                    throw new ConflictException("Заявка уже в архиве");
                }
                request.setIsArchived(true);
            }
            case RESTORE -> {
                if (!Boolean.TRUE.equals(request.getIsArchived())) {
                    throw new ConflictException("Заявка не в архиве");
                }
                request.setIsArchived(false);
            }
            case CHANGE_STATUS -> changeStatus(request, dto.status());
            case ADD_TAG -> {
                if (!request.getTags().add(tagRepository.getReferenceById(dto.tagId()))) {
                    throw new ConflictException("Тег уже добавлен");
                }
            }
            case REMOVE_TAG -> {
                if (!request.getTags().removeIf(t -> t.getId().equals(dto.tagId()))) {
                    throw new ConflictException("У заявки нет такого тега");
                }
            }
        }
        if (dto.action() != BulkAction.CHANGE_STATUS) {
            audit.record("REQUEST_" + dto.action().name(), EntityType.WALK_REQUEST, id,
                    "Массовая операция: " + dto.action());
        }
    }

    /** Правила переходов живут только в WalkRequestService, здесь они не дублируются. */
    private void changeStatus(WalkRequest request, RequestStatus target) {
        if (Boolean.TRUE.equals(request.getIsArchived())) {
            throw new ConflictException("Заявка в архиве");
        }
        switch (target) {
            case IN_PROGRESS -> requestService.start(request.getId());
            case COMPLETED -> requestService.complete(request.getId());
            case CANCELLED -> requestService.cancel(request.getId());
            default -> throw new BadRequestException("Нельзя вернуть заявку в статус " + target);
        }
    }

    private String message(Exception e) {
        if (e instanceof DataAccessException || e instanceof PersistenceException || e.getMessage() == null) {
            log.warn("Ошибка массовой операции", e);
            return "Не удалось выполнить операцию";          // подробности SQL клиенту не отдаём
        }
        return e.getMessage();
    }
}
