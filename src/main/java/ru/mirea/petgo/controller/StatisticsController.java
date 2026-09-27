package ru.mirea.petgo.controller;

import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.model.User;
import ru.mirea.petgo.model.enums.WalkStatus;
import ru.mirea.petgo.service.StatisticsService;
import java.util.Map;
import java.util.Scanner;

public class StatisticsController {
    private final Scanner scanner;
    private final StatisticsService statisticsService;

    public StatisticsController(Scanner scanner, StatisticsService statisticsService) {
        this.scanner = scanner;
        this.statisticsService = statisticsService;
    }

    private void totalUsers() {
        try {
            System.out.println("Всего пользователей: " + statisticsService.totalUsers());
        } catch (DatabaseException e) {
            System.out.println("Ошибка БД: " + e.getMessage());
        }
    }

    private void countWalkersAndOwners() {
        try {
            System.out.println("Выгульщиков: " + statisticsService.countWalkers());
            System.out.println("Владельцев: " + statisticsService.countOwners());

        } catch (DatabaseException e) {
            System.out.println("Ошибка БД: " + e.getMessage());
        }
    }

    private void countRequests() {
        try {
            System.out.println("Всего заявок: " + statisticsService.countRequests());
        } catch (DatabaseException e) {
            System.out.println("Ошибка БД: " + e.getMessage());
        }
    }

    private void requestsByStatus() {
        try {
            System.out.println("Заявки по статусам: ");
            Map<WalkStatus, Integer> byStatus = statisticsService.requestsByStatus();
            for (Map.Entry<WalkStatus, Integer> entry : byStatus.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            }
        } catch (DatabaseException e) {
            System.out.println("Ошибка БД: " + e.getMessage());
        }
    }

    private void averagePrice() {
        try {
            System.out.println("Средняя стоимость выгула: " + statisticsService.averagePrice() + " руб.");
        } catch (DatabaseException e) {
            System.out.println("Ошибка БД: " + e.getMessage());
        }
    }

    private void countPets() {
        try {
            System.out.println("Всего питомцев: " + statisticsService.countPets());
        } catch (DatabaseException e) {
            System.out.println("Ошибка БД: " + e.getMessage());
        }
    }

    private void mostActiveWalker() {
        try {
            User top = statisticsService.mostActiveWalker();
            if (top == null) {
                System.out.println("Нет данных о завершённых прогулках");
            } else {
                System.out.println("Самый активный выгульщик: " + top.getName() + " (ID: " + top.getId() + ")");
            }
        } catch (DatabaseException e) {
            System.out.println("Ошибка базы данных: " + e.getMessage());
        }
    }

    private void showAll() {
        try {
            System.out.println("\n=== СТАТИСТИКА PETGO ===");
            System.out.println("Всего пользователей: " + statisticsService.totalUsers());
            System.out.println("Владельцев: " + statisticsService.countOwners());
            System.out.println("Выгульщиков: " + statisticsService.countWalkers());
            System.out.println("Всего питомцев: " + statisticsService.countPets());
            System.out.println("Всего заявок: " + statisticsService.countRequests());
            System.out.println("Средняя стоимость: " + statisticsService.averagePrice() + " руб.");
            System.out.println("\nРаспределение по статусам:");
            Map<WalkStatus, Integer> byStatus = statisticsService.requestsByStatus();
            for (Map.Entry<WalkStatus, Integer> entry : byStatus.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            }

            User top = statisticsService.mostActiveWalker();
            System.out.println("\nСамый активный выгульщик: "
                    + (top != null ? top.getName() : "нет данных"));
        } catch (DatabaseException e) {
            System.out.println("Ошибка базы данных: " + e.getMessage());
        }
    }

    public void ShowMenu() {
        boolean running = true;
        while (running) {
            try {
                System.out.println("""
                        ========= СТАТИСТИКА СИСТЕМЫ =========
                        1. Общее количество пользователей
                        2. Количество владельцев и выгульщиков
                        3. Общее количество заявок
                        4. Распределение заявок по статусам
                        5. Средняя стоимость выгула
                        6. Количество питомцев
                        7. Самый активный выгульщик
                        8. Показать всю статистику
                        9. Назад
                        ======================================
                        """);
                System.out.print("Выберите действие: ");
                String choice = scanner.nextLine();

                switch (choice) {
                    case "1":
                        totalUsers();
                        break;
                    case "2":
                        countWalkersAndOwners();
                        break;
                    case "3":
                        countRequests();
                        break;
                    case "4":
                        requestsByStatus();
                        break;
                    case "5":
                        averagePrice();
                        break;
                    case "6":
                        countPets();
                        break;
                    case "7":
                        mostActiveWalker();
                        break;
                    case "8":
                        showAll();
                        break;
                    case "9":
                        running = false;
                        break;
                    default:
                        System.out.println("Такого пункта нет.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Введите корректные данные: " + e.getMessage());
            }
        }
    }
}
