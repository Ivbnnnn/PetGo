package ru.mirea.petgo;

import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.exception.EntityNotFoundException;
import ru.mirea.petgo.model.WalkHistory;
import ru.mirea.petgo.model.WalkRequest;
import ru.mirea.petgo.repository.PetRepository;
import ru.mirea.petgo.repository.UserRepository;
import ru.mirea.petgo.repository.WalkHistoryRepository;
import ru.mirea.petgo.repository.WalkRequestRepository;
import ru.mirea.petgo.service.WalkHistoryService;
import ru.mirea.petgo.service.WalkRequestService;
import ru.mirea.petgo.util.DatabaseManager;

import java.sql.Connection;

public class Main {
    public static void main(String[] args) throws Exception {
        try (Connection conn = DatabaseManager.getConnection()) {
            System.out.println("Подключение к БД работает");

            WalkRequestRepository walkRequestRepo = new WalkRequestRepository(conn);
            WalkHistoryRepository walkHistoryRepo = new WalkHistoryRepository(conn);
            PetRepository petRepo = new PetRepository(conn);
            UserRepository userRepo = new UserRepository(conn);

            WalkRequestService walkRequestService =
                    new WalkRequestService(walkRequestRepo, petRepo, userRepo);
            WalkHistoryService walkHistoryService =
                    new WalkHistoryService(walkHistoryRepo, walkRequestRepo);

            // проверка WalkRequestService
            try {
                WalkRequest request = walkRequestService.findById(6);
                System.out.println("Заявка найдена: id = " + request.getId()
                        + ", статус - " + request.getStatus()
                        + ", адрес - " + request.getWalkAddress());
            } catch (EntityNotFoundException e) {
                System.out.println(e.getMessage());
            } catch (DatabaseException e) {
                System.out.println(e.getMessage());
            }

            System.out.println("Всего заявок: " + walkRequestService.findAll().size());

            // рповерка WalkHistoryService
            try {
                WalkHistory history = walkHistoryService.findById(6);
                System.out.println("История найдена: id = " + history.getId()
                        + ", рейтинг - " + history.getRating()
                        + ", отзыв - " + history.getOwnerReview());
            } catch (EntityNotFoundException e) {
                System.out.println(e.getMessage());
            } catch (DatabaseException e) {
                System.out.println(e.getMessage());
            }

            System.out.println("Всего записей истории: " + walkHistoryService.findAll().size());
        }
    }
}