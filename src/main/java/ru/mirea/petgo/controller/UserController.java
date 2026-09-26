package ru.mirea.petgo.controller;

import ru.mirea.petgo.exception.BusinessException;
import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.exception.EntityNotFoundException;
import ru.mirea.petgo.model.User;
import ru.mirea.petgo.model.enums.UserRole;
import ru.mirea.petgo.service.UserService;

import java.util.List;
import java.util.Scanner;

public class UserController {
    private final Scanner scanner;
    private final UserService userService;

    public UserController(Scanner scanner, UserService userService) {
        this.scanner = scanner;
        this.userService = userService;
    }

    private void createUserMenu() {
        try {
            User user = readUserData(new User());
            user.setActive(true);

            userService.create(user);
            System.out.println("Пользователь добавлен. ID: " + user.getId());

        } catch (IllegalArgumentException e) {
            System.out.println("Роль должна быть OWNER или WALKER.");
        } catch (BusinessException e) {
            System.out.println(e.getMessage());
        } catch (DatabaseException e) {
            System.out.println("Ошибка базы данных: " + e.getMessage());
        }
    }

    private void showAllUsers() {
        try {
            List<User> users = userService.findAll();

            if (users.isEmpty()) {
                System.out.println("Пользователей нет.");
                return;
            }

            System.out.println("Список пользователей:");
            for (User user : users) {
                printUser(user);
            }

        } catch (DatabaseException e) {
            System.out.println("Ошибка базы данных: " + e.getMessage());
        }
    }

    private void getUserById() {
        try {
            System.out.print("Введите ID пользователя: ");
            int userId = Integer.parseInt(scanner.nextLine());

            User user = userService.findById(userId);
            System.out.println("Пользователь найден:");
            printUser(user);

        } catch (NumberFormatException e) {
            System.out.println("ID должен быть числом.");
        } catch (EntityNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (DatabaseException e) {
            System.out.println("Ошибка базы данных: " + e.getMessage());
        }
    }

    private void updateUser() {
        try {
            System.out.print("Введите ID пользователя для обновления: ");
            int userId = Integer.parseInt(scanner.nextLine());

            User user = userService.findById(userId);
            System.out.println("Введите новые данные пользователя:");
            readUserData(user);

            System.out.print("Пользователь активен? (true или false): ");
            String activeText = scanner.nextLine();

            if (activeText.equalsIgnoreCase("true")) {
                user.setActive(true);
            } else if (activeText.equalsIgnoreCase("false")) {
                user.setActive(false);
            } else {
                System.out.println("Введите true или false.");
                return;
            }

            userService.update(user);
            System.out.println("Пользователь обновлён.");

        } catch (NumberFormatException e) {
            System.out.println("ID должен быть числом.");
        } catch (IllegalArgumentException e) {
            System.out.println("Роль должна быть OWNER или WALKER.");
        } catch (EntityNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (BusinessException e) {
            System.out.println(e.getMessage());
        } catch (DatabaseException e) {
            System.out.println("Ошибка базы данных: " + e.getMessage());
        }
    }

    private void deleteUser() {
        try {
            System.out.print("Введите ID пользователя для удаления: ");
            int userId = Integer.parseInt(scanner.nextLine());

            userService.delete(userId);
            System.out.println("Пользователь удалён.");

        } catch (NumberFormatException e) {
            System.out.println("ID должен быть числом.");
        } catch (EntityNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (DatabaseException e) {
            System.out.println("Ошибка базы данных: " + e.getMessage());
        }
    }

    private User readUserData(User user) {
        System.out.print("Введите имя: ");
        user.setName(scanner.nextLine());

        System.out.print("Введите email: ");
        user.setEmail(scanner.nextLine());

        System.out.print("Введите телефон: ");
        user.setPhone(scanner.nextLine());

        System.out.print("Введите роль (OWNER или WALKER): ");
        String roleText = scanner.nextLine();
        user.setRole(UserRole.valueOf(roleText.toUpperCase()));

        System.out.print("Введите адрес: ");
        user.setAddress(scanner.nextLine());

        return user;
    }

    private void printUser(User user) {
        System.out.println(
                "ID: " + user.getId()
                        + ", имя: " + user.getName()
                        + ", email: " + user.getEmail()
                        + ", телефон: " + user.getPhone()
                        + ", роль: " + user.getRole()
                        + ", активен: " + user.isActive()
        );
    }

    public void ShowMenu() {
        boolean running = true;

        while (running) {
            System.out.println("""
                    ========= УПРАВЛЕНИЕ ПОЛЬЗОВАТЕЛЯМИ =========
                    1. Добавить пользователя
                    2. Показать всех пользователей
                    3. Получить пользователя по ID
                    4. Обновить пользователя
                    5. Удалить пользователя
                    6. Поиск пользователей
                    7. Назад
                    ================================================
                    """);
            System.out.print("Выберите действие: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    createUserMenu();
                    break;
                case "2":
                    showAllUsers();
                    break;
                case "3":
                    getUserById();
                    break;
                case "4":
                    updateUser();
                    break;
                case "5":
                    deleteUser();
                    break;
                case "6":
                    break;
                case "7":
                    running = false;
                    break;
                default:
                    System.out.println("Такого пункта нет.");
            }
        }
    }
}
