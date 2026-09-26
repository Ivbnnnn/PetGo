package ru.mirea.petgo;

import ru.mirea.petgo.controller.MainController;
import ru.mirea.petgo.exception.BusinessException;
import ru.mirea.petgo.exception.DatabaseException;

import ru.mirea.petgo.model.Pet;
import ru.mirea.petgo.model.WalkRequest;
import ru.mirea.petgo.model.enums.WalkStatus;

import ru.mirea.petgo.repository.PetRepository;
import ru.mirea.petgo.repository.UserRepository;
import ru.mirea.petgo.repository.WalkHistoryRepository;
import ru.mirea.petgo.repository.WalkRequestRepository;

import ru.mirea.petgo.service.WalkHistoryService;
import ru.mirea.petgo.service.WalkRequestService;
import ru.mirea.petgo.service.ExportService;
import ru.mirea.petgo.service.UserService;
// import ru.mirea.petgo.service.PetService;

import ru.mirea.petgo.controller.MainController;
import ru.mirea.petgo.controller.UserController;

import ru.mirea.petgo.util.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args)  {
        try (Connection conn = DatabaseManager.getConnection()) {
            Scanner scanner = new Scanner(System.in);
            // создаем сущности: репозитории, сервисы и контроллеры
            PetRepository petRepository = new PetRepository(conn);
            UserRepository userRepository = new UserRepository(conn);
            WalkHistoryRepository walkHistoryRepository = new WalkHistoryRepository(conn);
            WalkRequestRepository walkRequestRepository = new WalkRequestRepository(conn);

            // PetService petService = new PetService();
            UserService userService = new UserService(userRepository);
            WalkHistoryService walkHistoryService = new WalkHistoryService(walkHistoryRepository, walkRequestRepository);
            WalkRequestService walkRequestService = new WalkRequestService(walkRequestRepository, petRepository, userRepository);

            UserController userController = new UserController(scanner, userService);
            boolean running = true;
            
            while (running){
                MainController.MainMenu();
                String choice = scanner.nextLine();
                switch (choice) {
                    case "1":
                        userController.ShowMenu();
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