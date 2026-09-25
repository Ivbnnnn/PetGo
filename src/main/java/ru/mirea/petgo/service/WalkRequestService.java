package ru.mirea.petgo.service;

import ru.mirea.petgo.exception.BusinessException;
import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.exception.EntityNotFoundException;
import ru.mirea.petgo.model.Pet;
import ru.mirea.petgo.model.User;
import ru.mirea.petgo.model.WalkRequest;
import ru.mirea.petgo.model.enums.UserRole;
import ru.mirea.petgo.model.enums.WalkStatus;
import ru.mirea.petgo.repository.PetRepository;
import ru.mirea.petgo.repository.UserRepository;
import ru.mirea.petgo.repository.WalkRequestRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

        Pet pet;
        try {
            pet = petRepository.findById(request.getPetId());
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска питомца", e);
        }
        if (pet == null) {
            throw new BusinessException("Питомец с id=" + request.getPetId() + " не найден");
        }
        if (pet.getOwnerId() != request.getOwnerId()) {
            throw new BusinessException("Питомец не принадлежит этому владельцу");
        }

        User owner;
        try {
            owner = userRepository.findById(request.getOwnerId());
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска владельца", e);
        }
        if (owner == null) {
            throw new BusinessException("Владелец с id=" + request.getOwnerId() + " не найден");
        }
        if (owner.getRole() != UserRole.OWNER) {
            throw new BusinessException("Пользователь не является владельцем");
        }

        request.setStatus(WalkStatus.CREATED);
        request.setWalkerId(null);
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

        try {
            boolean updated = walkRequestRepository.update(request);
            if (!updated) {
                throw new BusinessException("Не удалось обновить заявку");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка обновления заявки", e);
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


    public WalkRequest acceptRequest(int requestId, int walkerId)
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

        if (request.getStatus() != WalkStatus.PENDING && request.getStatus() != WalkStatus.CREATED) {
            throw new BusinessException("Нельзя взять заявку в статусе " + request.getStatus());
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
        request.setStatus(WalkStatus.CONFIRMED);
        request.setUpdatedAt(LocalDateTime.now());

        try {
            walkRequestRepository.update(request);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка обновления заявки", e);
        }
        return request;
    }

    public WalkRequest startWalk(int requestId, int walkerId)
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

        if (request.getStatus() != WalkStatus.CONFIRMED) {
            throw new BusinessException("Начать прогулку можно только из статуса CONFIRMED");
        }
        if (request.getWalkerId() == null || request.getWalkerId() != walkerId) {
            throw new BusinessException("Эту заявку выполняет другой выгульщик");
        }

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

    private void validateRequest(WalkRequest request) throws BusinessException {
        if (request == null) {
            throw new BusinessException("Заявка не может быть null");
        }
        if (request.getPetId() <= 0) {
            throw new BusinessException("Некорректный id питомца");
        }
        if (request.getOwnerId() <= 0) {
            throw new BusinessException("Некорректный id владельца");
        }
        if (request.getWalkDateTime() == null) {
            throw new BusinessException("Дата и время прогулки обязательны");
        }
        if (request.getWalkDateTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Нельзя создать заявку на прошедшее время");
        }
        if (request.getDurationMinutes() <= 0) {
            throw new BusinessException("Длительность должна быть положительной");
        }
        if (request.getWalkAddress() == null || request.getWalkAddress().isBlank()) {
            throw new BusinessException("Адрес прогулки обязателен");
        }
    }
}