
package ru.mirea.petgo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.mirea.petgo.domain.RequestStatus;
import ru.mirea.petgo.domain.WalkHistory;
import ru.mirea.petgo.domain.WalkRequest;
import ru.mirea.petgo.dto.WalkHistoryDto;
import ru.mirea.petgo.error.BadRequestException;
import ru.mirea.petgo.error.ConflictException;
import ru.mirea.petgo.error.ResourceNotFoundException;
import ru.mirea.petgo.repository.WalkHistoryRepository;
import ru.mirea.petgo.repository.WalkRequestRepository;

import jakarta.persistence.criteria.Predicate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalkHistoryService {

    private final WalkHistoryRepository historyRepository;
    private final WalkRequestRepository requestRepository;

    /**
     * Создаёт историю при завершении прогулки.
     * Повторное выполнение не создаёт дубликат.
     */
    @Transactional
    public void ensureCreated(WalkRequest request) {

        if (request.getStatus() != RequestStatus.COMPLETED) {
            throw new ConflictException(
                    "Историю можно создать только для завершённой прогулки"
            );
        }

        if (historyRepository.existsByWalkRequest_Id(
                request.getId())) {
            return;
        }

        WalkHistory history = new WalkHistory();

        history.setWalkRequest(request);

        // Начальное значение до уточнения фактических данных.
        history.setActualDuration(
                request.getDurationMinutes()
        );

        history.setCompletedAt(LocalDateTime.now());

        historyRepository.saveAndFlush(history);
    }

    /**
     * Получение истории конкретной заявки.
     */
    @Transactional(readOnly = true)
    public WalkHistoryDto getByRequestId(UUID requestId) {

        WalkHistory history = historyRepository
                .findByWalkRequest_Id(requestId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "История прогулки не найдена"
                        )
                );

        return toDto(history);
    }

    /**
     * Просмотр истории с фильтрами и пагинацией.
     */
    @Transactional(readOnly = true)
    public Page<WalkHistoryDto> getAll(
            UUID requestId,
            Integer minRating,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {

        if (minRating != null &&
                (minRating < 1 || minRating > 5)) {
            throw new BadRequestException(
                    "Оценка должна быть от 1 до 5"
            );
        }

        if (from != null &&
                to != null &&
                from.isAfter(to)) {
            throw new BadRequestException(
                    "Начало периода позже окончания"
            );
        }

        if (pageable.getPageSize() > 100) {
            throw new BadRequestException(
                    "Максимальный размер страницы — 100"
            );
        }

        Specification<WalkHistory> specification =
                (root, query, cb) -> {

            List<Predicate> conditions = new ArrayList<>();

            if (requestId != null) {
                conditions.add(
                        cb.equal(
                                root.get("walkRequest").get("id"),
                                requestId
                        )
                );
            }

            if (minRating != null) {
                conditions.add(
                        cb.ge(
                                root.get("rating"),
                                minRating
                        )
                );
            }

            if (from != null) {
                conditions.add(
                        cb.greaterThanOrEqualTo(
                                root.get("completedAt"),
                                from
                        )
                );
            }

            if (to != null) {
                conditions.add(
                        cb.lessThanOrEqualTo(
                                root.get("completedAt"),
                                to
                        )
                );
            }

            return cb.and(
                    conditions.toArray(new Predicate[0])
            );
        };

        Pageable safePage = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(
                        Sort.Direction.DESC,
                        "completedAt"
                )
        );

        return historyRepository.findAll(
                specification,
                safePage
        ).map(this::toDto);
    }

    /**
     * Сохранение фактических данных завершённой прогулки.
     * Если запись уже есть, обновляет её.
     */
    @Transactional
    public WalkHistoryDto createOrUpdate(
            UUID requestId,
            WalkHistoryDto dto
    ) {

        validate(dto);

        WalkRequest request = requestRepository
                .findById(requestId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Заявка не найдена"
                        )
                );

        if (request.getStatus() != RequestStatus.COMPLETED) {
            throw new ConflictException(
                    "Прогулка ещё не завершена"
            );
        }

        checkHistoryPermission(request);

        WalkHistory history = historyRepository
                .findByWalkRequest_Id(requestId)
                .orElseGet(() -> {
                    WalkHistory newHistory = new WalkHistory();
                    newHistory.setWalkRequest(request);
                    newHistory.setCompletedAt(
                            LocalDateTime.now()
                    );
                    return newHistory;
                });

        applyChanges(history, dto);

        return toDto(
                historyRepository.saveAndFlush(history)
        );
    }

    /**
     * Редактирование истории по UUID.
     */
    @Transactional
    public WalkHistoryDto update(
            UUID historyId,
            WalkHistoryDto dto
    ) {

        WalkHistory history = historyRepository
                .findById(historyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "История прогулки не найдена"
                        )
                );

        validate(dto);

        WalkRequest request = history.getWalkRequest();

        if (request.getStatus() != RequestStatus.COMPLETED) {
            throw new ConflictException(
                    "Изменять можно только историю завершённой прогулки"
            );
        }

        checkHistoryPermission(request);

        applyChanges(history, dto);

        return toDto(
                historyRepository.saveAndFlush(history)
        );
    }

    /**
     * Применение изменений с учётом роли участника.
     */
    private void applyChanges(
            WalkHistory history,
            WalkHistoryDto dto
    ) {

        WalkRequest request = history.getWalkRequest();

        if (isAdmin()) {
            applyAllFields(history, dto);
            return;
        }

        UUID userId = currentUserId();

        boolean owner = request.getOwner()
                .getId().equals(userId);

        boolean walker = request.getWalker() != null &&
                request.getWalker().getId().equals(userId);

        if (owner) {
            if (!Objects.equals(
                    history.getActualDuration(),
                    dto.actualDuration()
            ) ||
                !Objects.equals(
                    history.getRoute(),
                    dto.route()
                ) ||
                !Objects.equals(
                    history.getWalkerReview(),
                    dto.walkerReview()
                )) {
                throw new AccessDeniedException(
                        "Владелец может изменять только свой отзыв и оценку"
                );
            }

            history.setOwnerReview(dto.ownerReview());
            history.setRating(dto.rating());
            return;
        }

        if (walker) {
            if (!Objects.equals(
                    history.getOwnerReview(),
                    dto.ownerReview()
            ) ||
                !Objects.equals(
                    history.getRating(),
                    dto.rating()
                )) {
                throw new AccessDeniedException(
                        "Выгульщик не может изменять отзыв и оценку владельца"
                );
            }

            history.setActualDuration(dto.actualDuration());
            history.setRoute(dto.route());
            history.setWalkerReview(dto.walkerReview());
            return;
        }

        throw new AccessDeniedException(
                "Недостаточно прав для изменения истории"
        );
    }

    private void applyAllFields(
            WalkHistory history,
            WalkHistoryDto dto
    ) {
        history.setActualDuration(dto.actualDuration());
        history.setRoute(dto.route());
        history.setOwnerReview(dto.ownerReview());
        history.setWalkerReview(dto.walkerReview());
        history.setRating(dto.rating());
    }

    /**
     * Проверка допуска к изменению истории.
     */
    private void checkHistoryPermission(
            WalkRequest request
    ) {

        if (!hasEditorAccess()) {
            throw new AccessDeniedException(
                    "Для изменения истории нужен уровень EDITOR или ADMIN"
            );
        }

        if (isAdmin()) {
            return;
        }

        UUID userId = currentUserId();

        boolean owner = request.getOwner()
                .getId().equals(userId);

        boolean walker = request.getWalker() != null &&
                request.getWalker().getId().equals(userId);

        if (!owner && !walker) {
            throw new AccessDeniedException(
                    "Изменять историю могут только участники прогулки"
            );
        }
    }

    private boolean hasEditorAccess() {

        Authentication auth = currentAuthentication();

        return auth.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_EDITOR")
                        || authority.getAuthority().equals("ROLE_ADMIN")
                );
    }

    private boolean isAdmin() {

        Authentication auth = currentAuthentication();

        return auth.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")
                );
    }

    private UUID currentUserId() {

        Authentication auth = currentAuthentication();

        if (!(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException(
                    "Требуется JWT-авторизация"
            );
        }

        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException e) {
            throw new AccessDeniedException(
                    "Некорректный идентификатор пользователя"
            );
        }
    }

    private Authentication currentAuthentication() {

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException(
                    "Требуется авторизация"
            );
        }

        return auth;
    }

    /**
     * Проверка входных данных истории.
     */
    private void validate(WalkHistoryDto dto) {

        if (dto == null) {
            throw new BadRequestException(
                    "Данные истории не переданы"
            );
        }

        if (dto.actualDuration() == null ||
                dto.actualDuration() <= 0) {
            throw new BadRequestException(
                    "Фактическая длительность должна быть больше нуля"
            );
        }

        if (dto.rating() != null &&
                (dto.rating() < 1 || dto.rating() > 5)) {
            throw new BadRequestException(
                    "Оценка должна быть от 1 до 5"
            );
        }
    }

    /**
     * Преобразование JPA-сущности в DTO.
     */
    private WalkHistoryDto toDto(
            WalkHistory history
    ) {

        return new WalkHistoryDto(
                history.getId(),
                history.getWalkRequest().getId(),
                history.getActualDuration(),
                history.getRoute(),
                history.getOwnerReview(),
                history.getWalkerReview(),
                history.getRating(),
                history.getCompletedAt()
        );
    }
}
