package ru.mirea.petgo;

import ru.mirea.petgo.controller.DatabaseController;
import ru.mirea.petgo.controller.ExportController;
import ru.mirea.petgo.controller.FilterRequestController;
import ru.mirea.petgo.controller.MainController;
import ru.mirea.petgo.controller.PetController;
import ru.mirea.petgo.controller.SortRequestController;
import ru.mirea.petgo.controller.UserController;
import ru.mirea.petgo.controller.WalkHistoryController;
import ru.mirea.petgo.controller.WalkRequestController;
import ru.mirea.petgo.controller.StatisticsController;
import ru.mirea.petgo.repository.PetRepository;
import ru.mirea.petgo.repository.UserRepository;
import ru.mirea.petgo.repository.WalkHistoryRepository;
import ru.mirea.petgo.repository.WalkRequestRepository;
import ru.mirea.petgo.service.DatabaseService;
import ru.mirea.petgo.service.ExportService;
import ru.mirea.petgo.service.PetService;
import ru.mirea.petgo.service.StatisticsService;
import ru.mirea.petgo.service.UserService;
import ru.mirea.petgo.service.WalkHistoryService;
import ru.mirea.petgo.service.WalkRequestService;
import ru.mirea.petgo.util.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try (Connection connection = DatabaseManager.getConnection();
             Scanner scanner = new Scanner(System.in)) {

            PetRepository petRepository = new PetRepository(connection);
            UserRepository userRepository = new UserRepository(connection);
            WalkHistoryRepository walkHistoryRepository = new WalkHistoryRepository(connection);
            WalkRequestRepository walkRequestRepository = new WalkRequestRepository(connection);

            PetService petService = new PetService(petRepository, userRepository);
            UserService userService = new UserService(userRepository);
            WalkHistoryService walkHistoryService = new WalkHistoryService(walkHistoryRepository, walkRequestRepository);
            WalkRequestService walkRequestService = new WalkRequestService(walkRequestRepository, petRepository, userRepository);
            StatisticsService statisticsService = new StatisticsService(userRepository, petRepository, walkRequestRepository);
            ExportService exportService = new ExportService(walkRequestRepository, userRepository, petRepository);
            DatabaseService databaseService = new DatabaseService();

            UserController userController = new UserController(scanner, userService);
            PetController petController = new PetController(scanner, petService);
            WalkRequestController walkRequestController = new WalkRequestController(scanner, walkRequestService);
            FilterRequestController filterRequestController = new FilterRequestController(scanner, walkRequestService);
            SortRequestController sortRequestController = new SortRequestController(scanner, walkRequestService);
            StatisticsController statisticsController = new StatisticsController(scanner, statisticsService);
            WalkHistoryController walkHistoryController = new WalkHistoryController(scanner, walkHistoryService);
            ExportController exportController = new ExportController(scanner, exportService);
            DatabaseController databaseController = new DatabaseController(scanner, databaseService);
            boolean running = true;

            while (running) {
                MainController.MainMenu();
                String choice = scanner.nextLine();

                switch (choice) {
                    case "1":
                        userController.ShowMenu();
                        break;
                    case "2":
                        petController.showMenu();
                        break;
                    case "3":
                        walkRequestController.showMenu();
                        break;
                    case "4":
                        filterRequestController.showMenu();
                        break;
                    case "5":
                        sortRequestController.showMenu();
                        break;
                    case "6":
                        walkHistoryController.showMenu();
                        break;
                    case "7":
                        statisticsController.ShowMenu();
                        break;
                    case "8":
                        exportController.showMenu();
                        break;
                    case "9":
                        databaseController.showMenu();
                        break;
                    case "0":
                        running = false;
                        break;
                    default:
                        System.out.println("Нет такого пункта");
                }
            }
        } catch (SQLException e) {
            System.out.println("Ошибка подключения к БД");
            System.out.println(e.getMessage());
        }
    }
}
