package ru.mirea.petgo.service;

import ru.mirea.petgo.exception.BusinessException;
import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.exception.EntityNotFoundException;
import ru.mirea.petgo.exception.ValidationException;
import ru.mirea.petgo.model.Pet;
import ru.mirea.petgo.model.User;
import ru.mirea.petgo.model.WalkRequest;
import ru.mirea.petgo.model.enums.UserRole;
import ru.mirea.petgo.model.enums.WalkStatus;
import ru.mirea.petgo.repository.PetRepository;
import ru.mirea.petgo.repository.UserRepository;
import ru.mirea.petgo.repository.WalkRequestRepository;
import ru.mirea.petgo.util.Validators;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import ru.mirea.petgo.dto.WalkRequestRow;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

public class WalkRequestService {

    private final WalkRequestRepository walkRequestRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;

    public WalkRequestService(WalkRequestRepository walkRequestRepository,
            PetRepository petRepository,
            UserRepository userRepository) {
        this.walkRequestRepository = walkRequestRepository;
        this.petRepository = petRepository;
        this.userRepository = userRepository;
    }

    public WalkRequest create(WalkRequest request) throws BusinessException, DatabaseException {
        validateRequest(request);
        applyOwnerFromPet(request);
        validateRequestRelations(request, true);

        if (request.getWalkDateTime().isBefore(LocalDateTime.now())) {
            throw new ValidationException("walkDateTime", "Нельзя создать заявку на прошедшее время");
        }

        request.setStatus(WalkStatus.CREATED);
        request.setCreatedAt(LocalDateTime.now());

        try {
            return walkRequestRepository.save(request);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка сохранения заявки", e);
        }
    }

    public WalkRequest findById(int id) throws EntityNotFoundException, DatabaseException {
        if (id <= 0) {
            throw new EntityNotFoundException("ID должен быть положительным");
        }
        WalkRequest request;
        try {
            request = walkRequestRepository.findById(id);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска заявки", e);
        }
        if (request == null) {
            throw new EntityNotFoundException("Заявка с id=" + id + " не найдена");
        }
        return request;
    }

    public List<WalkRequest> findAll() throws DatabaseException {
        try {
            return walkRequestRepository.findAll();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка получения списка заявок", e);
        }
    }

    public void update(WalkRequest request) throws BusinessException, DatabaseException {
        if (request.getId() <= 0) {
            throw new BusinessException("Нельзя обновить заявку без ID");
        }
        validateRequest(request);

        WalkRequest existing;
        try {
            existing = walkRequestRepository.findById(request.getId());
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска заявки", e);
        }
        if (existing == null) {
            throw new BusinessException("Заявка с id=" + request.getId() + " не найдена");
        }
        if (existing.getStatus() != request.getStatus()) {
            throw new BusinessException("Для изменения статуса используйте startWalk / completeWalk / cancelRequest");
        }
        if (!request.getWalkDateTime().equals(existing.getWalkDateTime())
                && request.getWalkDateTime().isBefore(LocalDateTime.now()))
            throw new ValidationException("walkDateTime", "Нельзя перенести заявку на прошедшее время");
        applyOwnerFromPet(request);
        validateRequestRelations(request, false);
        try {
            boolean updated = walkRequestRepository.update(request);
            if (!updated) {
                throw new BusinessException("Не удалось обновить заявку");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка обновления заявки", e);
        }
    }

    private void applyOwnerFromPet(WalkRequest request) throws BusinessException, DatabaseException {
        try {
            Pet pet = petRepository.findById(request.getPetId());
            if (pet == null) {
                throw new ValidationException("pet", "Питомец не найден");
            }
            request.setOwnerId(pet.getOwnerId());
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска питомца", e);
        }
    }

    private void validateRequestRelations(WalkRequest request, boolean creating)
            throws BusinessException, DatabaseException {
        try {
            User owner = userRepository.findById(request.getOwnerId());
            if (owner == null)
                throw new ValidationException("pet", "Владелец питомца не найден");
            if (owner.getRole() != UserRole.OWNER)
                throw new ValidationException("pet", "Владелец питомца не имеет роли «Владелец»");
            if (creating && !owner.isActive())
                throw new ValidationException("pet", "Владелец питомца неактивен, создать заявку нельзя");

            if (request.getWalkerId() != null) {
                User walker = userRepository.findById(request.getWalkerId());
                if (walker == null)
                    throw new ValidationException("walker", "Выгульщик не найден");
                if (walker.getRole() != UserRole.WALKER)
                    throw new ValidationException("walker", "Выбранный пользователь не является выгульщиком");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка проверки связей заявки", e);
        }
    }

    public void delete(int id) throws EntityNotFoundException, DatabaseException {
        try {
            if (walkRequestRepository.findById(id) == null) {
                throw new EntityNotFoundException("Заявка с id=" + id + " не найдена");
            }
            walkRequestRepository.deleteById(id);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка удаления заявки", e);
        }
    }

    public WalkRequest startWalk(int requestId, int walkerId) throws BusinessException, DatabaseException {
        WalkRequest request;
        try {
            request = walkRequestRepository.findById(requestId);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска заявки", e);
        }
        if (request == null) {
            throw new BusinessException("Заявка с id=" + requestId + " не найдена");
        }

        if (request.getStatus() != WalkStatus.CREATED) {
            throw new BusinessException("Начать прогулку можно только из статуса CREATED");
        }
        if (request.getWalkerId() != null && request.getWalkerId() != walkerId) {
            throw new BusinessException("Эту заявку выполняет другой выгульщик");
        }
        User walker;
        try {
            walker = userRepository.findById(walkerId);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска выгульщика", e);
        }
        if (walker == null) {
            throw new BusinessException("Выгульщик с id=" + walkerId + " не найден");
        }
        if (walker.getRole() != UserRole.WALKER) {
            throw new BusinessException("Пользователь не является выгульщиком");
        }
        request.setWalkerId(walkerId);
        request.setStatus(WalkStatus.IN_PROGRESS);
        request.setUpdatedAt(LocalDateTime.now());

        try {
            walkRequestRepository.update(request);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка обновления заявки", e);
        }
        return request;
    }

    public WalkRequest completeWalk(int requestId, int walkerId)
            throws BusinessException, DatabaseException {
        WalkRequest request;
        try {
            request = walkRequestRepository.findById(requestId);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска заявки", e);
        }
        if (request == null) {
            throw new BusinessException("Заявка с id=" + requestId + " не найдена");
        }

        if (request.getStatus() != WalkStatus.IN_PROGRESS) {
            throw new BusinessException("Завершить можно только прогулку в статусе IN_PROGRESS");
        }
        if (request.getWalkerId() == null || request.getWalkerId() != walkerId) {
            throw new BusinessException("Эту заявку выполняет другой выгульщик");
        }

        request.setStatus(WalkStatus.COMPLETED);
        request.setUpdatedAt(LocalDateTime.now());

        try {
            walkRequestRepository.update(request);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка обновления заявки", e);
        }
        return request;
    }

    public WalkRequest cancelRequest(int requestId, int userId)
            throws BusinessException, DatabaseException {
        WalkRequest request;
        try {
            request = walkRequestRepository.findById(requestId);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска заявки", e);
        }
        if (request == null) {
            throw new BusinessException("Заявка с id=" + requestId + " не найдена");
        }

        if (request.getStatus() == WalkStatus.COMPLETED
                || request.getStatus() == WalkStatus.CANCELLED) {
            throw new BusinessException("Нельзя отменить заявку в статусе " + request.getStatus());
        }

        boolean isOwner = request.getOwnerId() == userId;
        boolean isWalker = request.getWalkerId() != null && request.getWalkerId() == userId;
        if (!isOwner && !isWalker) {
            throw new BusinessException("Пользователь не имеет отношения к этой заявке");
        }

        request.setStatus(WalkStatus.CANCELLED);
        request.setUpdatedAt(LocalDateTime.now());

        try {
            walkRequestRepository.update(request);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка обновления заявки", e);
        }
        return request;
    }

    public WalkRequest startWalk(int requestId) throws BusinessException, DatabaseException {
        WalkRequest r = getExisting(requestId);
        if (r.getWalkerId() == null) {
            throw new BusinessException("Сначала назначьте выгульщика");
        }
        return startWalk(requestId, r.getWalkerId());
    }

    public WalkRequest completeWalk(int requestId) throws BusinessException, DatabaseException {
        WalkRequest r = getExisting(requestId);
        if (r.getWalkerId() == null) {
            throw new BusinessException("У заявки не назначен выгульщик");
        }
        return completeWalk(requestId, r.getWalkerId());
    }

    public WalkRequest cancelRequest(int requestId) throws BusinessException, DatabaseException {
        WalkRequest r = getExisting(requestId);
        return cancelRequest(requestId, r.getOwnerId());
    }

    private WalkRequest getExisting(int requestId) throws BusinessException, DatabaseException {
        try {
            WalkRequest r = walkRequestRepository.findById(requestId);
            if (r == null) {
                throw new BusinessException("Заявка с id=" + requestId + " не найдена");
            }
            return r;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска заявки", e);
        }
    }

    public List<WalkRequest> findByPetName(String petName)
            throws BusinessException, DatabaseException {
        if (petName == null || petName.isBlank()) {
            throw new BusinessException("Имя питомца не может быть пустым");
        }
        try {
            return walkRequestRepository.findByPetName(petName);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска по имени питомца", e);
        }
    }

    public List<WalkRequest> findByDate(LocalDate date)
            throws BusinessException, DatabaseException {
        if (date == null) {
            throw new BusinessException("Дата обязательна");
        }
        try {
            return walkRequestRepository.findByDate(date);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска по дате", e);
        }
    }

    public List<WalkRequest> findByStatus(WalkStatus status) throws DatabaseException {
        try {
            return walkRequestRepository.findByStatus(status);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска по статусу", e);
        }
    }

    public List<WalkRequest> findByOwnerId(int ownerId) throws DatabaseException {
        try {
            return walkRequestRepository.findByOwnerId(ownerId);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска по владельцу", e);
        }
    }

    public List<WalkRequest> findAllSorted(String field, boolean asc)
            throws BusinessException, DatabaseException {
        try {
            return walkRequestRepository.findAllSorted(field, asc);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка сортировки заявок", e);
        }
    }

    public List<WalkRequest> findAllSortedByPetName(boolean asc) throws DatabaseException {
        try {
            return walkRequestRepository.findAllSortedByPetName(asc);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка сортировки по имени питомца", e);
        }
    }

    public List<WalkRequestRow> findAllRows() throws DatabaseException {
        try {
            Map<Integer, String> pets = petRepository.findAll().stream()
                    .collect(Collectors.toMap(Pet::getId, Pet::getName));
            Map<Integer, String> users = userRepository.findAll().stream()
                    .collect(Collectors.toMap(User::getId, User::getName));
            List<WalkRequestRow> rows = new ArrayList<>();
            for (WalkRequest r : walkRequestRepository.findAll()) {
                rows.add(new WalkRequestRow(
                        r.getId(),
                        r.getPetId(), pets.get(r.getPetId()),
                        r.getOwnerId(), users.get(r.getOwnerId()),
                        r.getWalkerId(),
                        r.getWalkerId() == null ? null : users.get(r.getWalkerId()),
                        r.getWalkDateTime(),
                        r.getDurationMinutes(),
                        r.getWalkAddress(),
                        r.getStatus(),
                        r.getDescription(),
                        r.getPrice()));
            }
            return rows;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка получения списка заявок", e);
        }
    }

    private void validateRequest(WalkRequest request) throws BusinessException {
        if (request == null) {
            throw new BusinessException("Заявка не может быть null");
        }
        if (request.getPetId() <= 0) {
            throw new ValidationException("pet", "Выберите питомца");
        }
        if (request.getWalkDateTime() == null)
            throw new ValidationException("walkDateTime", "Укажите дату и время прогулки");
        if (request.getDurationMinutes() <= 0 || request.getDurationMinutes() > 1440) {
            throw new ValidationException("duration", "Длительность: от 1 до 1440 минут");
        }
        Validators.requireText("walkAddress", "Адрес", request.getWalkAddress(), 200);
        if (request.getPrice() != null)
            Validators.range("price", "Цена", request.getPrice(), BigDecimal.ZERO, new BigDecimal("99999999.99"));
    }
}
