package ru.mirea.petgo;

import ru.mirea.petgo.model.User;
import ru.mirea.petgo.repository.UserRepository;
import ru.mirea.petgo.util.DatabaseManager;

import java.sql.Connection;

public class Main {
    public static void main(String[] args) throws Exception {
        try (Connection conn = DatabaseManager.getConnection()) {
            System.out.println("✅ Подключение к БД работает");

            UserRepository userRepo = new UserRepository(conn);
            User user = userRepo.findById(1);
            System.out.println("Найден: " + user.getName() + " (" + user.getEmail() + ")");
            System.out.println("Всего пользователей: " + userRepo.findAll().size());
        }
    }
}