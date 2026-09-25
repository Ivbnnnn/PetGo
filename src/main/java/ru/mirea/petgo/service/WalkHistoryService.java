package ru.mirea.petgo.service;

import ru.mirea.petgo.exception.BusinessException;
import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.exception.EntityNotFoundException;
import ru.mirea.petgo.model.WalkHistory;
import ru.mirea.petgo.model.WalkRequest;
import ru.mirea.petgo.model.enums.WalkStatus;
import ru.mirea.petgo.repository.WalkHistoryRepository;
import ru.mirea.petgo.repository.WalkRequestRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WalkHistoryService {

    private final WalkHistoryRepository walkHistoryRepository;
    private final WalkRequestRepository walkRequestRepository;

    public WalkHistoryService(WalkHistoryRepository walkHistoryRepository,
                              WalkRequestRepository walkRequestRepository) {
        this.walkHistoryRepository = walkHistoryRepository;
        this.walkRequestRepository = walkRequestRepository;
    }

    public WalkHistory create(WalkHistory history) throws BusinessException, DatabaseException {
        validateHistory(history);

        WalkRequest request;
        try {
            request = walkRequestRepository.findById(history.getWalkRequestId());
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска заявки", e);
        }
        if (request == null) {
            throw new BusinessException("Заявка с id=" + history.getWalkRequestId() + " не найдена");
        }
        if (request.getStatus() != WalkStatus.IN_PROGRESS) {
            throw new BusinessException("Историю можно создать только для прогулки в статусе IN_PROGRESS");
        }

        WalkHistory existing;
        try {
            existing = findHistoryByRequestId(history.getWalkRequestId());
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска истории", e);
        }
        if (existing != null) {
            throw new BusinessException("История для этой заявки уже существует");
        }

        history.setCompletedAt(LocalDateTime.now());

        WalkHistory saved;
        try {
            saved = walkHistoryRepository.save(history);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка сохранения истории", e);
        }

        request.setStatus(WalkStatus.COMPLETED);
        request.setUpdatedAt(LocalDateTime.now());
        try {
            walkRequestRepository.update(request);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка обновления заявки", e);
        }

        return saved;
    }

    public WalkHistory findById(int id) throws EntityNotFoundException, DatabaseException {
        if (id <= 0) {
            throw new EntityNotFoundException("ID должен быть положительным");
        }
        WalkHistory history;
        try {
            history = walkHistoryRepository.findById(id);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска истории", e);
        }
        if (history == null) {
            throw new EntityNotFoundException("История с id=" + id + " не найдена");
        }
        return history;
    }

    public List<WalkHistory> findAll() throws DatabaseException {
        try {
            return walkHistoryRepository.findAll();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка получения списка истории", e);
        }
    }

    public void update(WalkHistory history) throws BusinessException, DatabaseException {
        if (history.getId() <= 0) {
            throw new BusinessException("Нельзя обновить историю без ID");
        }
        validateHistory(history);

        WalkHistory existing;
        try {
            existing = walkHistoryRepository.findById(history.getId());
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска истории", e);
        }
        if (existing == null) {
            throw new BusinessException("История с id=" + history.getId() + " не найдена");
        }

        try {
            boolean updated = walkHistoryRepository.update(history);
            if (!updated) {
                throw new BusinessException("Не удалось обновить историю");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка обновления истории", e);
        }
    }

    public void delete(int id) throws EntityNotFoundException, DatabaseException {
        try {
            if (walkHistoryRepository.findById(id) == null) {
                throw new EntityNotFoundException("История с id=" + id + " не найдена");
            }
            walkHistoryRepository.deleteById(id);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка удаления истории", e);
        }
    }

    public WalkHistory addOwnerReview(int historyId, String review, Integer rating)
            throws BusinessException, DatabaseException, EntityNotFoundException {
        if (review == null || review.isBlank()) {
            throw new BusinessException("Отзыв не может быть пустым");
        }
        validateRating(rating);

        WalkHistory history = findById(historyId);
        history.setOwnerReview(review);
        if (rating != null) {
            history.setRating(rating);
        }

        update(history);
        return history;
    }

    public WalkHistory addWalkerReview(int historyId, String review)
            throws BusinessException, DatabaseException, EntityNotFoundException {
        if (review == null || review.isBlank()) {
            throw new BusinessException("Отзыв не может быть пустым");
        }

        WalkHistory history = findById(historyId);
        history.setWalkerReview(review);
        update(history);
        return history;
    }

    public double averageRating() throws DatabaseException {
        List<WalkHistory> all = findAll();
        int count = 0;
        int sum = 0;
        for (WalkHistory h : all) {
            if (h.getRating() != null) {
                sum += h.getRating();
                count++;
            }
        }
        if (count == 0) {
            return 0.0;
        }
        return (double) sum / count;
    }

    public WalkHistory findByRequest(int walkRequestId) throws DatabaseException {
        try {
            return findHistoryByRequestId(walkRequestId);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска истории по заявке", e);
        }
    }

    public List<WalkHistory> findAllWithRating(int minRating) throws DatabaseException {
        List<WalkHistory> result = new ArrayList<>();
        for (WalkHistory h : findAll()) {
            if (h.getRating() != null && h.getRating() >= minRating) {
                result.add(h);
            }
        }
        return result;
    }

    private WalkHistory findHistoryByRequestId(int walkRequestId) throws SQLException {
        for (WalkHistory h : walkHistoryRepository.findAll()) {
            if (h.getWalkRequestId() == walkRequestId) {
                return h;
            }
        }
        return null;
    }

    private void validateHistory(WalkHistory history) throws BusinessException {
        if (history == null) {
            throw new BusinessException("История не может быть null");
        }
        if (history.getWalkRequestId() <= 0) {
            throw new BusinessException("Некорректный id заявки");
        }
        if (history.getActualDuration() != null && history.getActualDuration() < 0) {
            throw new BusinessException("Длительность не может быть отрицательной");
        }
        validateRating(history.getRating());
    }

    private void validateRating(Integer rating) throws BusinessException {
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new BusinessException("Рейтинг должен быть от 1 до 5");
        }
    }
}