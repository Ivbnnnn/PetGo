package ru.mirea.petgo.service;

import ru.mirea.petgo.exception.BusinessException;
import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.exception.EntityNotFoundException;
import ru.mirea.petgo.exception.ValidationException;
import ru.mirea.petgo.model.WalkHistory;
import ru.mirea.petgo.model.WalkRequest;
import ru.mirea.petgo.model.enums.WalkStatus;
import ru.mirea.petgo.repository.WalkHistoryRepository;
import ru.mirea.petgo.repository.WalkRequestRepository;
import ru.mirea.petgo.util.Validators;
import ru.mirea.petgo.dto.WalkHistoryRow;
import ru.mirea.petgo.repository.PetRepository;
import java.math.BigDecimal;
import ru.mirea.petgo.model.Pet;
import java.util.stream.Collectors;
import java.util.Map;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WalkHistoryService {

    private final WalkHistoryRepository walkHistoryRepository;
    private final WalkRequestRepository walkRequestRepository;
    private final PetRepository petRepository;

    public WalkHistoryService(WalkHistoryRepository walkHistoryRepository,
            WalkRequestRepository walkRequestRepository, PetRepository petRepository) {
        this.walkHistoryRepository = walkHistoryRepository;
        this.walkRequestRepository = walkRequestRepository;
        this.petRepository = petRepository;
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
        if (request.getStatus() != WalkStatus.COMPLETED) {
            throw new BusinessException(
                    "Историю можно создать только для прогулки в статусе COMPLETED. Текущий статус: "
                            + request.getStatus());
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

        try {
            return walkHistoryRepository.save(history);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка сохранения истории", e);
        }
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

    public List<WalkHistoryRow> findAllRows() throws DatabaseException {
        try {
            Map<Integer, String> pets = petRepository.findAll().stream()
                    .collect(Collectors.toMap(Pet::getId, Pet::getName));
            Map<Integer, WalkRequest> requests = walkRequestRepository.findAll().stream()
                    .collect(Collectors.toMap(WalkRequest::getId, r -> r));

            List<WalkHistoryRow> rows = new ArrayList<>();
            for (WalkHistory h : walkHistoryRepository.findAll()) {
                WalkRequest r = requests.get(h.getWalkRequestId());
                rows.add(new WalkHistoryRow(
                        h.getId(),
                        h.getWalkRequestId(),
                        r == null ? "—" : pets.get(r.getPetId()),
                        r == null ? null : r.getWalkDateTime(),
                        h.getActualDuration(),
                        h.getRoute(),
                        h.getOwnerReview(),
                        h.getWalkerReview(),
                        h.getRating(),
                        h.getCompletedAt()));
            }
            return rows;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка получения истории прогулок", e);
        }
    }

    private void validateHistory(WalkHistory history) throws BusinessException {
        if (history == null) {
            throw new BusinessException("История не может быть null");
        }
        if (history.getWalkRequestId() <= 0) {
            throw new BusinessException("Некорректный id заявки");
        }
        if (history.getActualDuration() != null && history.getActualDuration() <= 0) {
            throw new ValidationException("actualDuration", "Длительность должна быть больше 0");
        }
        validateRating(history.getRating());
    }

    private void validateRating(Integer rating) throws BusinessException {
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new ValidationException("rating", "Оценка: от 1 до 5");
        }
    }
}