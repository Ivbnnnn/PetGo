package ru.mirea.petgo.controller;

import java.util.List;
import java.util.Scanner;

import ru.mirea.petgo.exception.BusinessException;
import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.exception.EntityNotFoundException;
import ru.mirea.petgo.model.WalkHistory;
import ru.mirea.petgo.service.WalkHistoryService;

public class WalkHistoryController {
    private final Scanner scanner;
    private final WalkHistoryService walkHistoryService;

    public WalkHistoryController(Scanner scanner, WalkHistoryService walkHistoryService) {
        this.scanner = scanner;
        this.walkHistoryService = walkHistoryService;
    }

    private void createWalkHistory() {
        try {
            WalkHistory history = readHistoryData(new WalkHistory());
            walkHistoryService.create(history);
            System.out.println("История прогулки создана.");
        } catch (BusinessException e) {
            System.out.println(e.getMessage());
        } catch (DatabaseException e) {
            System.out.println(e.getMessage());
        }
    }

    private void showAllHistories() {
        try {
            List<WalkHistory> histories = walkHistoryService.findAll();

            if (histories.isEmpty()) {
                System.out.println("Истории прогулок нет.");
                return;
            }

            for (WalkHistory history : histories) {
                printHistory(history);
            }
        } catch (DatabaseException e) {
            System.out.println(e.getMessage());
        }
    }

    private void printHistory(WalkHistory history) {
        System.out.println(
                "ID: " + history.getId()
                        + ", ID заявки: " + history.getWalkRequestId()
                        + ", фактическая длительность: " + history.getActualDuration()
                        + ", маршрут: " + history.getRoute()
                        + ", отзыв владельца: " + history.getOwnerReview()
                        + ", отзыв выгульщика: " + history.getWalkerReview()
                        + ", оценка: " + history.getRating()
                        + ", завершена: " + history.getCompletedAt()
        );
    }

    private WalkHistory readHistoryData(WalkHistory history) {
        System.out.print("Введите ID заявки: ");
        try {
            history.setWalkRequestId(Integer.parseInt(scanner.nextLine()));
        } catch (NumberFormatException e) {
            System.out.println("Введите корректный ID заявки.");
        }

        System.out.print("Введите фактическую длительность в минутах: ");
        try {
            history.setActualDuration(Integer.parseInt(scanner.nextLine()));
        } catch (NumberFormatException e) {
            System.out.println("Введите корректную длительность.");
        }

        System.out.print("Введите маршрут: ");
        history.setRoute(scanner.nextLine());

        System.out.print("Введите отзыв владельца: ");
        history.setOwnerReview(scanner.nextLine());

        System.out.print("Введите отзыв выгульщика: ");
        history.setWalkerReview(scanner.nextLine());

        System.out.print("Введите оценку от 1 до 5: ");
        try {
            history.setRating(Integer.parseInt(scanner.nextLine()));
        } catch (NumberFormatException e) {
            System.out.println("Введите корректную оценку.");
        }

        return history;
    }

    private void getHistoryById() {
        try {
            System.out.print("Введите ID истории: ");
            int historyId = Integer.parseInt(scanner.nextLine());

            WalkHistory history = walkHistoryService.findById(historyId);
            printHistory(history);
        } catch (NumberFormatException e) {
            System.out.println("Введите корректный ID.");
        } catch (EntityNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (DatabaseException e) {
            System.out.println(e.getMessage());
        }
    }

    private void updateHistory() {
        try {
            System.out.print("Введите ID истории для изменения: ");
            int historyId = Integer.parseInt(scanner.nextLine());

            WalkHistory history = walkHistoryService.findById(historyId);
            readHistoryData(history);
            walkHistoryService.update(history);
            System.out.println("История прогулки обновлена.");
        } catch (NumberFormatException e) {
            System.out.println("Введите корректный ID.");
        } catch (EntityNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (BusinessException e) {
            System.out.println(e.getMessage());
        } catch (DatabaseException e) {
            System.out.println(e.getMessage());
        }
    }

    private void deleteHistory() {
        try {
            System.out.print("Введите ID истории для удаления: ");
            int historyId = Integer.parseInt(scanner.nextLine());

            walkHistoryService.delete(historyId);
            System.out.println("История прогулки удалена.");
        } catch (NumberFormatException e) {
            System.out.println("Введите корректный ID.");
        } catch (EntityNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (DatabaseException e) {
            System.out.println(e.getMessage());
        }
    }

    private void addOwnerReview() {
        try {
            System.out.print("Введите ID истории: ");
            int historyId = Integer.parseInt(scanner.nextLine());

            System.out.print("Введите отзыв владельца: ");
            String review = scanner.nextLine();

            System.out.print("Введите оценку от 1 до 5: ");
            int rating = Integer.parseInt(scanner.nextLine());

            walkHistoryService.addOwnerReview(historyId, review, rating);
            System.out.println("Отзыв владельца добавлен.");
        } catch (NumberFormatException e) {
            System.out.println("ID и оценка должны быть числами.");
        } catch (EntityNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (BusinessException e) {
            System.out.println(e.getMessage());
        } catch (DatabaseException e) {
            System.out.println(e.getMessage());
        }
    }

    private void addWalkerReview() {
        try {
            System.out.print("Введите ID истории: ");
            int historyId = Integer.parseInt(scanner.nextLine());

            System.out.print("Введите отзыв выгульщика: ");
            String review = scanner.nextLine();

            walkHistoryService.addWalkerReview(historyId, review);
            System.out.println("Отзыв выгульщика добавлен.");
        } catch (NumberFormatException e) {
            System.out.println("Введите корректный ID.");
        } catch (EntityNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (BusinessException e) {
            System.out.println(e.getMessage());
        } catch (DatabaseException e) {
            System.out.println(e.getMessage());
        }
    }

    private void showAverageRating() {
        try {
            double average = walkHistoryService.averageRating();
            System.out.println("Средняя оценка: " + average);
        } catch (DatabaseException e) {
            System.out.println(e.getMessage());
        }
    }

    private void findHistoryByRequest() {
        try {
            System.out.print("Введите ID заявки: ");
            int requestId = Integer.parseInt(scanner.nextLine());

            WalkHistory history = walkHistoryService.findByRequest(requestId);
            if (history == null) {
                System.out.println("История для этой заявки не найдена.");
                return;
            }
            printHistory(history);
        } catch (NumberFormatException e) {
            System.out.println("Введите корректный ID заявки.");
        } catch (DatabaseException e) {
            System.out.println(e.getMessage());
        }
    }

    private void filterByRating() {
        try {
            System.out.print("Введите минимальную оценку от 1 до 5: ");
            int rating = Integer.parseInt(scanner.nextLine());

            if (rating < 1 || rating > 5) {
                System.out.println("Оценка должна быть от 1 до 5.");
                return;
            }

            List<WalkHistory> histories = walkHistoryService.findAllWithRating(rating);
            if (histories.isEmpty()) {
                System.out.println("Ничего не найдено.");
                return;
            }

            for (WalkHistory history : histories) {
                printHistory(history);
            }
        } catch (NumberFormatException e) {
            System.out.println("Введите корректную оценку.");
        } catch (DatabaseException e) {
            System.out.println(e.getMessage());
        }
    }

    public void showMenu() {
        boolean running = true;

        while (running) {
            System.out.println("""
                    ========= ИСТОРИЯ ПРОГУЛОК =========
                    1. Создать историю прогулки
                    2. Показать все истории прогулок
                    3. Получить историю по ID
                    4. Изменить историю
                    5. Удалить историю
                    6. Добавить отзыв владельца
                    7. Добавить отзыв выгульщика
                    8. Показать среднюю оценку
                    9. Найти историю по ID заявки
                    10. Фильтр по минимальной оценке
                    11. Назад
                    ====================================
                    """);

            System.out.print("Выберите действие: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    createWalkHistory();
                    break;
                case "2":
                    showAllHistories();
                    break;
                case "3":
                    getHistoryById();
                    break;
                case "4":
                    updateHistory();
                    break;
                case "5":
                    deleteHistory();
                    break;
                case "6":
                    addOwnerReview();
                    break;
                case "7":
                    addWalkerReview();
                    break;
                case "8":
                    showAverageRating();
                    break;
                case "9":
                    findHistoryByRequest();
                    break;
                case "10":
                    filterByRating();
                    break;
                case "11":
                    running = false;
                    break;
                default:
                    System.out.println("Такого пункта нет.");
            }
        }
    }
}
