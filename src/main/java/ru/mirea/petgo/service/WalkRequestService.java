
package ru.mirea.petgo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.mirea.petgo.domain.*;
import ru.mirea.petgo.domain.AuditEvent.EntityType;
import ru.mirea.petgo.dto.*;
import ru.mirea.petgo.error.*;
import ru.mirea.petgo.filter.PageSort;
import ru.mirea.petgo.filter.WalkRequestSpecification;
import ru.mirea.petgo.repository.*;
import ru.mirea.petgo.util.WalkRequestMapper;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalkRequestService {

        private static final Set<String> SORT_FIELDS = Set.of(
                        "id",
                        "externalId",
                        "scheduledAt",
                        "durationMinutes",
                        "address",
                        "price",
                        "status",
                        "priority",
                        "isArchived",
                        "createdAt",
                        "updatedAt");

        private final WalkRequestRepository requestRepository;
        private final PetRepository petRepository;
        private final UserRepository userRepository;
        private final WalkRequestMapper requestMapper;
        private final DistrictService districts;
        private final AuditService audit;
        private final WalkHistoryService historyService;

        @Value("${app.requests.permanent-delete-enabled:false}")
        private boolean permanentDeleteEnabled;

        @Transactional
        public WalkRequestResponseDto create(WalkRequestCreateDto dto) {

                Pet pet = petRepository.findById(dto.getPetId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Питомец не найден"));

                User owner = userRepository.findById(dto.getOwnerId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Владелец не найден"));

                if (owner.getRole() != UserRole.OWNER ||
                                !owner.isEnabled()) {
                        throw new ConflictException(
                                        "Создавать заявки может только активный владелец");
                }

                if (!owner.getId().equals(pet.getOwner().getId())) {
                        throw new ConflictException(
                                        "Питомец принадлежит другому владельцу");
                }

                requireCanManageRequest(owner.getId());

                validateDate(dto.getScheduledAt(), true);

                validateValues(
                                dto.getDurationMinutes(),
                                dto.getPrice(),
                                dto.getAddress());

                WalkRequest request = new WalkRequest();

                request.setExternalId(
                                "WR-" + UUID.randomUUID());

                request.setPet(pet);
                request.setOwner(owner);

                if (dto.getWalkerId() != null) {
                        request.setWalker(findWalker(dto.getWalkerId()));
                }

                assignDistrict(request, dto.getDistrictId());

                request.setScheduledAt(dto.getScheduledAt());
                request.setDurationMinutes(dto.getDurationMinutes());

                request.setPriority(
                                RequestPriority.forDuration(
                                                dto.getDurationMinutes()));

                request.setAddress(dto.getAddress().strip());
                request.setDescription(dto.getDescription());
                request.setPrice(dto.getPrice());
                request.setStatus(RequestStatus.CREATED);
                request.setIsArchived(false);

                requestRepository.saveAndFlush(request);

                audit.record(
                                "REQUEST_CREATED",
                                EntityType.WALK_REQUEST,
                                request.getId(),
                                "Создана заявка");

                return requestMapper.toDto(request);
        }

        @Transactional(readOnly = true)
        public WalkRequestResponseDto getById(UUID id) {

                WalkRequest request = findRequest(id);

                requireRequestReadAccess(request);

                return requestMapper.toDto(request);
        }

        @Transactional(readOnly = true)
        public Page<WalkRequestResponseDto> getAll(
                        WalkRequestFilterDto filter,
                        Pageable pageable) {

                Specification<WalkRequest> spec = WalkRequestSpecification.getFiltered(filter);

                spec = applyUserScope(spec);

                Pageable safePage = PageSort.apply(
                                pageable,
                                Sort.by(
                                                Sort.Direction.DESC,
                                                "scheduledAt"),
                                SORT_FIELDS);

                return requestRepository.findAll(spec, safePage)
                                .map(requestMapper::toDto);
        }

        @Transactional
        public WalkRequestResponseDto update(
                        UUID id,
                        WalkRequestUpdateDto dto) {

                WalkRequest request = findActiveRequest(id);

                requireCanManageRequest(
                                request.getOwner().getId());

                if (request.getStatus() == RequestStatus.COMPLETED ||
                                request.getStatus() == RequestStatus.CANCELLED) {

                        throw new ConflictException(
                                        "Завершённую или отменённую заявку нельзя изменять");
                }

                if (!request.getVersion().equals(dto.getVersion())) {
                        throw new ConflictException(
                                        "Заявка уже была изменена другим пользователем");
                }

                if (dto.getWalkerId() != null) {
                        request.setWalker(
                                        findWalker(dto.getWalkerId()));
                }

                if (dto.getDistrictId() != null) {
                        assignDistrict(
                                        request,
                                        dto.getDistrictId());
                }

                if (dto.getScheduledAt() != null) {
                        validateDate(
                                        dto.getScheduledAt(),
                                        true);

                        request.setScheduledAt(
                                        dto.getScheduledAt());
                }

                if (dto.getDurationMinutes() != null) {
                        request.setDurationMinutes(
                                        dto.getDurationMinutes());

                        request.setPriority(
                                        RequestPriority.forDuration(
                                                        dto.getDurationMinutes()));
                }

                if (dto.getAddress() != null) {
                        request.setAddress(
                                        dto.getAddress().strip());
                }

                if (dto.getDescription() != null) {
                        request.setDescription(
                                        dto.getDescription());
                }

                if (dto.getPrice() != null) {
                        request.setPrice(
                                        dto.getPrice());
                }

                validateValues(
                                request.getDurationMinutes(),
                                request.getPrice(),
                                request.getAddress());

                requestRepository.saveAndFlush(request);

                audit.record(
                                "REQUEST_UPDATED",
                                EntityType.WALK_REQUEST,
                                id,
                                "Заявка изменена");

                return requestMapper.toDto(request);
        }

        @Transactional
        public WalkRequestResponseDto start(UUID id) {

                WalkRequest request = findActiveRequest(id);

                if (request.getStatus() != RequestStatus.CREATED) {
                        throw new ConflictException(
                                        "Начать можно только созданную заявку");
                }

                if (request.getWalker() == null) {
                        throw new ConflictException(
                                        "Перед началом назначьте выгульщика");
                }

                requireWalkerOrAdmin(
                                request.getWalker().getId());

                return changeStatus(
                                request,
                                RequestStatus.IN_PROGRESS);
        }

        @Transactional
        public WalkRequestResponseDto complete(UUID id) {

                WalkRequest request = findActiveRequest(id);

                if (request.getStatus() != RequestStatus.IN_PROGRESS) {
                        throw new ConflictException(
                                        "Завершить можно только выполняемую прогулку");
                }

                if (request.getWalker() == null) {
                        throw new ConflictException(
                                        "У заявки отсутствует выгульщик");
                }

                requireWalkerOrAdmin(
                                request.getWalker().getId());

                request.setStatus(
                                RequestStatus.COMPLETED);

                requestRepository.saveAndFlush(request);

                historyService.ensureCreated(request);

                audit.record(
                                "REQUEST_STATUS_CHANGED",
                                EntityType.WALK_REQUEST,
                                id,
                                "Прогулка завершена");

                return requestMapper.toDto(request);
        }

        @Transactional
        public WalkRequestResponseDto cancel(UUID id) {

                WalkRequest request = findActiveRequest(id);

                requireCanManageRequest(
                                request.getOwner().getId());

                if (request.getStatus() == RequestStatus.COMPLETED ||
                                request.getStatus() == RequestStatus.CANCELLED) {

                        throw new ConflictException(
                                        "Заявку в этом статусе нельзя отменить");
                }

                return changeStatus(
                                request,
                                RequestStatus.CANCELLED);
        }

        @Transactional
        public void archive(UUID id) {

                WalkRequest request = findRequest(id);

                requireCanManageRequest(
                                request.getOwner().getId());

                if (!Boolean.TRUE.equals(request.getIsArchived())) {

                        request.setIsArchived(true);

                        requestRepository.saveAndFlush(request);

                        audit.record(
                                        "REQUEST_ARCHIVED",
                                        EntityType.WALK_REQUEST,
                                        id,
                                        "Заявка архивирована");
                }
        }

        @Transactional
        public void restore(UUID id) {

                WalkRequest request = findRequest(id);

                requireCanManageRequest(
                                request.getOwner().getId());

                if (Boolean.TRUE.equals(request.getIsArchived())) {

                        request.setIsArchived(false);

                        requestRepository.saveAndFlush(request);

                        audit.record(
                                        "REQUEST_RESTORED",
                                        EntityType.WALK_REQUEST,
                                        id,
                                        "Заявка восстановлена");
                }
        }

        @Transactional
        public void permanentDelete(UUID id) {

                if (!permanentDeleteEnabled) {
                        throw new AccessDeniedException(
                                        "Окончательное удаление отключено");
                }

                requireAdmin();

                WalkRequest request = findRequest(id);

                if (!Boolean.TRUE.equals(request.getIsArchived())) {
                        throw new ConflictException(
                                        "Сначала архивируйте заявку");
                }

                requestRepository.delete(request);
                requestRepository.flush();

                audit.record(
                                "REQUEST_DELETED",
                                EntityType.WALK_REQUEST,
                                id,
                                "Заявка окончательно удалена");
        }

        private WalkRequestResponseDto changeStatus(
                        WalkRequest request,
                        RequestStatus target) {

                request.setStatus(target);

                requestRepository.saveAndFlush(request);

                audit.record(
                                "REQUEST_STATUS_CHANGED",
                                EntityType.WALK_REQUEST,
                                request.getId(),
                                "Новый статус: " + target);

                return requestMapper.toDto(request);
        }

        private WalkRequest findRequest(UUID id) {

                return requestRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Заявка не найдена"));
        }

        private WalkRequest findActiveRequest(UUID id) {

                WalkRequest request = findRequest(id);

                if (Boolean.TRUE.equals(request.getIsArchived())) {
                        throw new ConflictException(
                                        "Нельзя изменять архивную заявку");
                }

                return request;
        }

        private User findWalker(UUID id) {

                User walker = userRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Выгульщик не найден"));

                if (walker.getRole() != UserRole.WALKER ||
                                !walker.isEnabled()) {

                        throw new ConflictException(
                                        "Требуется активный пользователь с ролью WALKER");
                }

                return walker;
        }

        private void assignDistrict(
                        WalkRequest request,
                        UUID id) {

                if (id != null &&
                                (request.getDistrict() == null ||
                                                !id.equals(request.getDistrict().getId()))) {

                        request.setDistrict(
                                        districts.assignable(id));
                }
        }

        private void validateDate(
                        LocalDateTime date,
                        boolean checkFuture) {

                if (date == null) {
                        throw new BadRequestException(
                                        "Дата прогулки обязательна");
                }

                if (checkFuture &&
                                date.isBefore(LocalDateTime.now())) {

                        throw new BadRequestException(
                                        "Дата прогулки не может быть в прошлом");
                }
        }

        private void validateValues(
                        Integer duration,
                        Double price,
                        String address) {

                if (duration == null || duration <= 0) {
                        throw new BadRequestException(
                                        "Длительность должна быть больше нуля");
                }

                if (price == null ||
                                !Double.isFinite(price) ||
                                price < 0 ||
                                price > 99999999.99) {

                        throw new BadRequestException(
                                        "Недопустимая цена заявки");
                }

                if (address == null ||
                                address.isBlank() ||
                                address.length() > 200) {

                        throw new BadRequestException(
                                        "Адрес обязателен и не должен превышать 200 символов");
                }
        }

        private UUID currentUserId() {

                Authentication authentication = SecurityContextHolder.getContext()
                                .getAuthentication();

                if (authentication == null ||
                                !(authentication.getPrincipal() instanceof Jwt jwt)) {

                        throw new AccessDeniedException(
                                        "Требуется авторизация");
                }

                return UUID.fromString(
                                jwt.getSubject());
        }

        private boolean hasAuthority(String authority) {

                Authentication authentication = SecurityContextHolder.getContext()
                                .getAuthentication();

                return authentication != null &&
                                authentication.getAuthorities().stream()
                                                .anyMatch(a -> a.getAuthority().equals(authority));
        }

        private boolean isAdmin() {
                return hasAuthority("ROLE_ADMIN");
        }

        private boolean isEditor() {
                return hasAuthority("ROLE_EDITOR");
        }

        private boolean isViewer() {
                return hasAuthority("ROLE_VIEWER");
        }

        private boolean isWalker() {
                return hasAuthority("ROLE_WALKER");
        }

        private void requireRequestReadAccess(WalkRequest request) {
                UUID userId = currentUserId();

                if (isAdmin() || isEditor()) {
                        return;
                }

                if (hasAuthority("ROLE_OWNER")) {
                        if (request.getOwner() != null &&
                                        userId.equals(request.getOwner().getId())) {
                                return;
                        }

                        throw new AccessDeniedException(
                                        "Нет доступа к чужой заявке");
                }

                if (isWalker()) {
                        if (request.getWalker() != null &&
                                        userId.equals(request.getWalker().getId())) {
                                return;
                        }

                        throw new AccessDeniedException(
                                        "Заявка не назначена этому выгульщику");
                }

                if (isViewer()) {
                        return;
                }

                throw new AccessDeniedException(
                                "Нет доступа к этой заявке");
        }

        private Specification<WalkRequest> applyUserScope(
                        Specification<WalkRequest> specification) {
                UUID userId = currentUserId();

                if (isAdmin() || isEditor()) {
                        return specification;
                }

                if (hasAuthority("ROLE_OWNER")) {
                        return specification.and((root, query, cb) -> cb.equal(root.get("owner").get("id"), userId));
                }

                if (isWalker()) {
                        return specification.and((root, query, cb) -> cb.equal(root.get("walker").get("id"), userId));
                }

                if (isViewer()) {
                        return specification;
                }

                throw new AccessDeniedException(
                                "Нет доступа к списку заявок");
        }

        private void requireCanManageRequest(UUID ownerId) {

                if (isAdmin() || isEditor()) {
                        return;
                }

                if (hasAuthority("ROLE_OWNER") &&
                                ownerId.equals(currentUserId())) {
                        return;
                }

                throw new AccessDeniedException(
                                "Изменять заявку может только её владелец, редактор или администратор");
        }

        private void requireWalkerOrAdmin(UUID walkerId) {

                if (isAdmin()) {
                        return;
                }

                if (isWalker() &&
                                walkerId.equals(currentUserId())) {
                        return;
                }

                throw new AccessDeniedException(
                                "Операция доступна только назначенному выгульщику или администратору");
        }

        private void requireAdmin() {

                if (!isAdmin()) {
                        throw new AccessDeniedException(
                                        "Операция доступна только администратору");
                }
        }
}
