package ru.mirea.petgo.controller;
import java.util.Scanner;
import ru.mirea.petgo.service.UserService;
import ru.mirea.petgo.model.User;
import ru.mirea.petgo.model.enums.UserRole;
import ru.mirea.petgo.exception.*;
import java.util.List;
public class UserController {

    private final Scanner scanner;
    private final UserService userService;

    public UserController(Scanner scanner, UserService userService){
        this.scanner = scanner;
        this.userService = userService;
    }


    private void createUserMenu() {
        System.out.print("Введите имя: ");
        String name = scanner.nextLine();

        System.out.print("Введите email: ");
        String email = scanner.nextLine();

        System.out.print("Введите телефон: ");
        String phone = scanner.nextLine();

        System.out.print("Введите роль (OWNER или WALKER): ");
        String roleText = scanner.nextLine();

        System.out.print("Введите адрес: ");
        String address = scanner.nextLine();

        try {
            UserRole role = UserRole.valueOf(roleText.toUpperCase());

            User newUser = new User();
            newUser.setName(name);
            newUser.setEmail(email);
            newUser.setPhone(phone);
            newUser.setRole(role);
            newUser.setAddress(address);
            newUser.setActive(true);

            userService.create(newUser);

            System.out.println("Пользователь успешно добавлен.");

        } catch (IllegalArgumentException e) {
            System.out.println("Роль должна быть OWNER или WALKER.");
        } catch (BusinessException e){
            System.out.println(e.getMessage());
        } catch (DatabaseException e){
            System.out.println(e.getMessage());
        }
    }
    
    private void deleteUser(){
        try {
            System.out.print("Введите ID пользователя для удаления: ");
            int userId = Integer.parseInt(scanner.nextLine());

            userService.delete(userId);
            System.out.println("Пользователь успешно удалён");
        } catch (EntityNotFoundException e){
            System.out.println(e.getMessage());
        } catch (DatabaseException e){
            System.out.println(e.getMessage());
        }
    }

    private void updateUser(){
        try {
            System.out.print("Введите ID пользователя для обновления: ");
            int userId = Integer.parseInt(scanner.nextLine());

            User existingUser = userService.findById(userId);
            
            System.out.println("Пользователь найден, введите новые данные");
            System.out.print("Введите имя: ");
            String name = scanner.nextLine();

            System.out.print("Введите email: ");
            String email = scanner.nextLine();

            System.out.print("Введите телефон: ");
            String phone = scanner.nextLine();

            System.out.print("Введите роль (OWNER или WALKER): ");
            String roleText = scanner.nextLine();

            System.out.print("Введите адрес: ");
            String address = scanner.nextLine();

            System.out.print("Введите статус пользователя: ");
            String activeText = scanner.nextLine();
            boolean active;
            if (activeText.equalsIgnoreCase("true")){
                active = true;
            } else if (activeText.equalsIgnoreCase("false")){
                active = false;
            } else{
                System.out.println("Введите статус в формате true или false");
                return ;
            }
            
            existingUser.setName(name);
            existingUser.setEmail(email);
            existingUser.setPhone(phone);
            if (!roleText.equalsIgnoreCase("OWNER")
                    && !roleText.equalsIgnoreCase("WALKER")) {
                System.out.println("Роль должна быть OWNER или WALKER.");
                return;
            }

            UserRole role = UserRole.valueOf(roleText.toUpperCase());
            existingUser.setRole(role);
            existingUser.setAddress(address);
            existingUser.setActive(active);
            
            userService.update(existingUser);
            System.out.println("Пользователь обновлён.");

        } catch (NumberFormatException e) {
            System.out.println("ID должен быть числом.");

        } catch (EntityNotFoundException e) {
            System.out.println(e.getMessage());

        } catch (DatabaseException e) {
            System.out.println("Ошибка базы данных: " + e.getMessage());
        } catch (BusinessException e) {
            System.out.println("Бизнес ошибка: " + e.getMessage());
        }
    }
    

    private void getUserById() {
        try {
            System.out.print("Введите ID пользователя: ");
            int userId = Integer.parseInt(scanner.nextLine());

            User user = userService.findById(userId);

            System.out.println("Пользователь найден:");
            System.out.println(
                    "ID: " + user.getId()
                    + ", имя: " + user.getName()
                    + ", email: " + user.getEmail()
                    + ", роль: " + user.getRole()
            );

        } catch (NumberFormatException e) {
            System.out.println("ID должен быть числом.");

        } catch (EntityNotFoundException e) {
            System.out.println(e.getMessage());

        } catch (DatabaseException e) {
            System.out.println("Ошибка базы данных: " + e.getMessage());
        }
    }

    private void showAllUsers(){
        try{
            List<User> users = userService.findAll();
            if (users.isEmpty()){
                System.out.println("Пользователей нет");
            }
            System.out.println("Список пользователей");
            for (User user: users){    
                System.out.println(
                    user.getId() + " | "
                    + user.getName() + " | "
                    + user.getEmail() + " | "
                    + user.getRole()
                );
                
            }
            System.out.println();
        }catch (DatabaseException e){
            System.out.print(e.getMessage());
        }
        
    }
    public void ShowMenu(){
        boolean running = true;

        while (running){
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
                    break;
            }
        }
        
    }
}
