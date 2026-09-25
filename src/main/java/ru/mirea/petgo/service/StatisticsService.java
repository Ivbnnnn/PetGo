package ru.mirea.petgo.service;

import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.model.User;
import ru.mirea.petgo.model.WalkRequest;
import ru.mirea.petgo.model.enums.UserRole;
import ru.mirea.petgo.model.enums.WalkStatus;
import ru.mirea.petgo.repository.PetRepository;
import ru.mirea.petgo.repository.UserRepository;
import ru.mirea.petgo.repository.WalkRequestRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StatisticsService {
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final WalkRequestRepository walkRequestRepository;

    public StatisticsService(UserRepository userRepository, PetRepository petRepository,
            WalkRequestRepository walkRequestRepository) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.walkRequestRepository = walkRequestRepository;
    }

    public int totalUsers() throws DatabaseException {
        try {
            return userRepository.findAll().size();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка подсчёта пользователей", e);
        }
    }

    public long countOwners() throws DatabaseException {
        try {
            return userRepository.findAll().stream().filter(u -> u.getRole() == UserRole.OWNER).count();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка подсчёта владельцев", e);
        }
    }

    public long countWalkers() throws DatabaseException {
        try {
            return userRepository.findAll().stream().filter(u -> u.getRole() == UserRole.WALKER).count();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка подсчёта выгульщиков", e);
        }
    }

    public int countPets() throws DatabaseException {
        try {
            return petRepository.findAll().size();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка подсчёта питомцев", e);
        }
    }

    public int countRequests() throws DatabaseException {
        try {
            return walkRequestRepository.findAll().size();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка подсчёта заявок", e);
        }
    }

    public Map<WalkStatus, Integer> requestsByStatus() throws DatabaseException {
        Map<WalkStatus, Integer> result = new LinkedHashMap<>();
        for (WalkStatus status : WalkStatus.values()) {
            result.put(status, 0);
        }
        try {
            for (WalkRequest r : walkRequestRepository.findAll()) {
                result.merge(r.getStatus(), 1, Integer::sum);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка подсчёта по статусам", e);
        }
        return result;
    }

    public BigDecimal averagePrice() throws DatabaseException {
        try {
            List<WalkRequest> all = walkRequestRepository.findAll();
            BigDecimal sum = BigDecimal.ZERO;
            int count = 0;
            for (WalkRequest r : all) {
                if (r.getPrice() != null) {
                    sum = sum.add(r.getPrice());
                    count++;
                }
            }
            if (count == 0)
                return BigDecimal.ZERO;
            return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка подсчёта средней цены", e);
        }
    }

    public User mostActiveWalker() throws DatabaseException {
        try {
            List<WalkRequest> completed = walkRequestRepository.findAll().stream()
                    .filter(r -> r.getStatus() == WalkStatus.COMPLETED).filter(r -> r.getWalkerId() != null).toList();

            if (completed.isEmpty())
                return null;

            Map<Integer, Long> counts = completed.stream().collect(java.util.stream.Collectors.groupingBy(
                    WalkRequest::getWalkerId, java.util.stream.Collectors.counting()));

            Integer topWalkerId = counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
                    .orElse(null);
            if (topWalkerId == null)
                return null;

            return userRepository.findById(topWalkerId);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска активного выгульщика", e);
        }
    }
}
