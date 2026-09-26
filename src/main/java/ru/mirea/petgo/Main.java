package ru.mirea.petgo;

import ru.mirea.petgo.controller.MainController;
import ru.mirea.petgo.controller.PetController;
import ru.mirea.petgo.controller.UserController;
import ru.mirea.petgo.repository.PetRepository;
import ru.mirea.petgo.repository.UserRepository;
import ru.mirea.petgo.repository.WalkHistoryRepository;
import ru.mirea.petgo.repository.WalkRequestRepository;
import ru.mirea.petgo.service.PetService;
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

            UserController userController = new UserController(scanner, userService);
            PetController petController = new PetController(scanner, petService);
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
                    default:
                        break;
                }
            }
        } catch (SQLException e) {
            System.out.println("Ошибка подключения к БД");
            System.out.println(e.getMessage());
        }
    }
}
