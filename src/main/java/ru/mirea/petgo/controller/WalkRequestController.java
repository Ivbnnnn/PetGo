package ru.mirea.petgo.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import java.util.List;
import ru.mirea.petgo.exception.BusinessException;
import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.exception.EntityNotFoundException;
import ru.mirea.petgo.model.WalkRequest;
import ru.mirea.petgo.model.enums.WalkStatus;
import ru.mirea.petgo.service.WalkRequestService;

public class WalkRequestController {

    private final Scanner scanner;
    private final WalkRequestService walkRequestService;

    public WalkRequestController(Scanner scanner, WalkRequestService walkRequestService) {
        this.scanner = scanner;
        this.walkRequestService = walkRequestService;
    }

    private void createRequest() {
        try {
            WalkRequest walkRequest = readRequestData(new WalkRequest());
            walkRequestService.create(walkRequest);
            System.out.println("Прогулка создана");
        } catch (BusinessException e) {
            System.out.println(e.getMessage());
        } catch (DatabaseException e) {
            System.out.println(e.getMessage());
        }

    }

    private void showAllRequests() {
        try {
            List<WalkRequest> requests = walkRequestService.findAll();
            if (requests.isEmpty()) {
                System.out.println("Прогулок нет");
                return;
            }
            for (WalkRequest req : requests) {
                printRequest(req);
            }
        } catch (DatabaseException e) {
            System.out.println(e.getMessage());
        }

    }

    private void findRequestById() {
        try {
            System.out.print("Введите Id прогулки: ");
            WalkRequest req = walkRequestService.findById(Integer.parseInt(scanner.nextLine()));
            printRequest(req);
        } catch (NumberFormatException e) {
            System.out.println("Введите корректный ID.");
        } catch (EntityNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (DatabaseException e) {
            System.out.println(e.getMessage());
        }
    }

    private void updateRequest() {
        try {
            System.out.print("Введите Id прогулки для обновления: ");
            WalkRequest req = walkRequestService.findById(Integer.parseInt(scanner.nextLine()));
            System.out.println("Введите новые данные");
            readRequestDataForUpdate(req);
            walkRequestService.update(req);
            System.out.println("Данные успешно обновлены");
        } catch (NumberFormatException e) {
            System.out.println("Введите корректный ID.");
        } catch (EntityNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (DatabaseException e) {
            System.out.println(e.getMessage());
        } catch (BusinessException e) {
            System.out.println(e.getMessage());
        }
    }

    private void deleteRequest() {
        try {
            System.out.print("Введите ID прогулки для удаления: ");
            walkRequestService.delete(Integer.parseInt(scanner.nextLine()));
            System.out.println("Заявка успешно удалена.");
        } catch (NumberFormatException e) {
            System.out.println("Введите корректный ID.");
        } catch (DatabaseException e) {
            System.out.println(e.getMessage());
        } catch (EntityNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    private void findRequest() {
        boolean running = true;
        while (running) {
            System.out.println("""
                    ========= ПОИСК ЗАЯВОК =========
                    1. По имени питомца
                    2. По дате
                    3. По статусу
                    4. Назад
                    =================================
                    """);
            System.out.print("Выберите действие: ");
            String choice = scanner.nextLine();

            try {
                switch (choice) {
                    case "1":
                        System.out.print("Имя питомца: ");
                        String name = scanner.nextLine();
                        List<WalkRequest> found = walkRequestService.findByPetName(name);
                        if (found.isEmpty()) {
                            System.out.println("Ничего не найдено");
                        } else {
                            found.forEach(this::printRequest);
                        }
                        break;

                    case "2":
                        System.out.print("Дата (yyyy-MM-dd): ");
                        try {
                            LocalDate date = LocalDate.parse(scanner.nextLine().trim());
                            List<WalkRequest> requests = walkRequestService.findByDate(date);
                            if (requests.isEmpty()) {
                                System.out.println("Ничего не найдено");
                            } else {
                                requests.forEach(this::printRequest);
                            }
                        } catch (java.time.format.DateTimeParseException e) {
                            System.out.println("Неверный формат даты. Пример: 2026-10-05");
                        }
                        break;
                    case "3":
                        System.out.print("Статус (CREATED / IN_PROGRESS / COMPLETED / CANCELLED): ");
                        try {
                            WalkStatus status = WalkStatus.valueOf(scanner.nextLine().trim().toUpperCase());
                            List<WalkRequest> find = walkRequestService.findByStatus(status);
                            if (find.isEmpty()) {
                                System.out.println("Ничего не найдено");
                            } else {
                                find.forEach(this::printRequest);
                            }
                        } catch (IllegalArgumentException e) {
                            System.out.println(
                                    "Нет такого статуса. Доступные: CREATED, IN_PROGRESS, COMPLETED, CANCELLED");
                        }
                        break;
                    case "4":
                        running = false;
                        break;
                    default:
                        System.out.println("Нет такого варианта");
                }
            } catch (BusinessException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (DatabaseException e) {
                System.out.println("Ошибка БД: " + e.getMessage());
            }
        }
    }

    private void printRequest(WalkRequest walkRequest) {
        System.out.println(
                "ID: " + walkRequest.getId() +
                        ", petId: " + walkRequest.getPetId() +
                        ", ownerId: " + walkRequest.getOwnerId() +
                        ", walkerId: " + walkRequest.getWalkerId() +
                        ", время: " + walkRequest.getWalkDateTime() +
                        ", длительность: " + walkRequest.getDurationMinutes() +
                        ", адрес: " + walkRequest.getWalkAddress() +
                        ", статус: " + walkRequest.getStatus() +
                        ", описание: " + walkRequest.getDescription());
    }

    private WalkRequest readRequestData(WalkRequest walkRequest) {
        System.out.print("Введите Id питомца: ");
        try {
            walkRequest.setPetId(Integer.parseInt(scanner.nextLine()));
        } catch (NumberFormatException e) {
            System.out.print("Введите корректный ID питомца");
        }
        System.out.print("Введите Id хозяина: ");
        try {
            walkRequest.setOwnerId(Integer.parseInt(scanner.nextLine()));
        } catch (NumberFormatException e) {
            System.out.print("Введите корректный ID хозяина");
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        System.out.print("Введите время прогулки: ");
        try {
            walkRequest.setWalkDateTime(LocalDateTime.parse(scanner.nextLine(), formatter));
        } catch (DateTimeParseException e) {
            System.out.print("Введите корректное время формата dd.MM.yyyy HH:mm.  ");
        }
        System.out.println("Введите длительность прогулки в минутах: ");
        try {
            walkRequest.setDurationMinutes(Integer.parseInt(scanner.nextLine()));
        } catch (NumberFormatException e) {
            System.out.print("Введите корректную длительность прогулки");
        }
        System.out.print("Введите адрес прогулки: ");
        walkRequest.setWalkAddress((scanner.nextLine()));
        System.out.print("Введите описание прогулки: ");
        walkRequest.setDescription((scanner.nextLine()));
        return walkRequest;
    }

    private WalkRequest readRequestDataForUpdate(WalkRequest walkRequest) {
        System.out.print("Введите Id питомца: ");
        try {
            walkRequest.setPetId(Integer.parseInt(scanner.nextLine()));
        } catch (NumberFormatException e) {
            System.out.print("Введите корректный ID питомца.  ");
        }
        System.out.print("Введите Id хозяина: ");
        try {
            walkRequest.setOwnerId(Integer.parseInt(scanner.nextLine()));
        } catch (NumberFormatException e) {
            System.out.print("Введите корректный ID хозяина");
        }
        System.out.print("Введите Id выгульщика: ");
        try {
            walkRequest.setWalkerId(Integer.parseInt(scanner.nextLine()));
        } catch (NumberFormatException e) {
            System.out.print("Введите корректный ID выгульщика");
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        System.out.print("Введите время прогулки: ");
        try {
            walkRequest.setWalkDateTime(LocalDateTime.parse(scanner.nextLine(), formatter));
        } catch (DateTimeParseException e) {
            System.out.print("Введите корректное время формата dd.MM.yyyy HH:mm");
        }
        System.out.print("Введите длительность прогулки в минутах: ");
        try {
            walkRequest.setDurationMinutes(Integer.parseInt(scanner.nextLine()));
        } catch (NumberFormatException e) {
            System.out.print("Введите корректную длительность прогулки");
        }
        System.out.print("Введите адрес прогулки: ");
        walkRequest.setWalkAddress((scanner.nextLine()));

        System.out.print("Введите описание прогулки: ");
        walkRequest.setDescription((scanner.nextLine()));
        return walkRequest;
    }

    private void startWalk() {
        try {
            System.out.print("ID заявки: ");
            int requestId = Integer.parseInt(scanner.nextLine());
            System.out.print("ID выгульщика: ");
            int walkerId = Integer.parseInt(scanner.nextLine());
            walkRequestService.startWalk(requestId, walkerId);
            System.out.println("Прогулка начата");
        } catch (NumberFormatException e) {
            System.out.println("ID должен быть числом");
        } catch (BusinessException e) {
            System.out.println(e.getMessage());
        } catch (DatabaseException e) {
            System.out.println("Ошибка БД: " + e.getMessage());
        }
    }

    private void completeWalk() {
        try {
            System.out.print("ID заявки: ");
            int requestId = Integer.parseInt(scanner.nextLine());
            System.out.print("ID выгульщика: ");
            int walkerId = Integer.parseInt(scanner.nextLine());
            walkRequestService.completeWalk(requestId, walkerId);
            System.out.println("Прогулка завершена");
        } catch (NumberFormatException e) {
            System.out.println("ID должен быть числом");
        } catch (BusinessException e) {
            System.out.println(e.getMessage());
        } catch (DatabaseException e) {
            System.out.println("Ошибка БД: " + e.getMessage());
        }
    }

    private void cancelRequest() {
        try {
            System.out.print("ID заявки: ");
            int requestId = Integer.parseInt(scanner.nextLine());
            System.out.print("ID пользователя: ");
            int userId = Integer.parseInt(scanner.nextLine());
            walkRequestService.cancelRequest(requestId, userId);
            System.out.println("Заявка отменена");
        } catch (NumberFormatException e) {
            System.out.println("ID должен быть числом");
        } catch (BusinessException e) {
            System.out.println(e.getMessage());
        } catch (DatabaseException e) {
            System.out.println("Ошибка БД: " + e.getMessage());
        }
    }

    public void showMenu() {
        boolean running = true;

        while (running) {
            System.out.println("""
                    ========= УПРАВЛЕНИЕ ЗАЯВКАМИ =========
                    1. Создать заявку
                    2. Показать все заявки
                    3. Получить заявку по ID
                    4. Обновить заявку
                    5. Удалить заявку
                    6. Поиск заявок
                    7. Начать прогулку
                    8. Завершить прогулку
                    9. Отменить прогулку
                    10. Назад
                    ========================================
                    """);

            System.out.print("Выберите действие: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    createRequest();
                    break;
                case "2":
                    showAllRequests();
                    break;
                case "3":
                    findRequestById();
                    break;
                case "4":
                    updateRequest();
                    break;
                case "5":
                    deleteRequest();
                    break;
                case "6":
                    findRequest();
                    break;
                case "7":
                    startWalk();
                    break;
                case "8":
                    completeWalk();
                    break;
                case "9":
                    cancelRequest();
                    break;
                case "10":
                    running = false;
                    break;
                default:
                    System.out.println("Такого пункта нет.");
            }

        }
    }
}
