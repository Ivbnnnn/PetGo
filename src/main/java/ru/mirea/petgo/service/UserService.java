package ru.mirea.petgo.service;

import ru.mirea.petgo.exception.BusinessException;
import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.exception.EntityNotFoundException;
import ru.mirea.petgo.model.User;
import ru.mirea.petgo.repository.UserRepository;

import java.sql.SQLException;
import java.util.List;

public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User create(User user) throws BusinessException, DatabaseException {
        validateUser(user);

        if (findByEmailOrNull(user.getEmail()) != null) {
            throw new BusinessException("Пользователь с email " + user.getEmail() + " уже существует");
        }

        try {
            return userRepository.save(user);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка сохранения пользователя", e);
        }
    }

    public User findById(int id) throws EntityNotFoundException, DatabaseException {
        if (id <= 0) {
            throw new EntityNotFoundException("ID должен быть положительным числом");
        }
        try {
            User user = userRepository.findById(id);
            if (user == null) {
                throw new EntityNotFoundException("Пользователь с id=" + id + " не найден");
            }
            return user;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска пользователя по id=" + id, e);
        }
    }

    public List<User> findAll() throws DatabaseException {
        try {
            return userRepository.findAll();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка получения списка пользователей", e);
        }
    }

    public void update(User user)
            throws BusinessException, EntityNotFoundException, DatabaseException {
        if (user.getId() <= 0) {
            throw new BusinessException("Нельзя обновить пользователя без ID");
        }
        validateUser(user);

        findById(user.getId());

        User existing = findByEmailOrNull(user.getEmail());
        if (existing != null && existing.getId() != user.getId()) {
            throw new BusinessException("Email " + user.getEmail() + " занят другим пользователем");
        }

        try {
            boolean updated = userRepository.update(user);
            if (!updated) {
                throw new EntityNotFoundException("Пользователь с id=" + user.getId() + " не найден");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка обновления пользователя id=" + user.getId(), e);
        }
    }

    public void delete(int id) throws EntityNotFoundException, DatabaseException {
        findById(id);

        try {
            boolean deleted = userRepository.deleteById(id);
            if (!deleted) {
                throw new EntityNotFoundException("Пользователь с id=" + id + " не найден");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка удаления пользователя id=" + id, e);
        }
    }

    public User findByEmail(String email)
            throws BusinessException, EntityNotFoundException, DatabaseException {
        if (email == null || email.isBlank()) {
            throw new BusinessException("Email не может быть пустым");
        }
        try {
            User user = userRepository.findByEmail(email);
            if (user == null) {
                throw new EntityNotFoundException("Пользователь с email " + email + " не найден");
            }
            return user;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска по email " + email, e);
        }
    }

    public List<User> findByName(String namePart)
            throws BusinessException, DatabaseException {
        if (namePart == null || namePart.isBlank()) {
            throw new BusinessException("Строка для поиска не может быть пустой");
        }
        try {
            return userRepository.findByName(namePart);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска по имени " + namePart, e);
        }
    }

    private User findByEmailOrNull(String email) throws DatabaseException {
        try {
            return userRepository.findByEmail(email);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска по email", e);
        }
    }

    private void validateUser(User user) throws BusinessException {
        if (user == null) {
            throw new BusinessException("Пользователь не может быть null");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            throw new BusinessException("Имя пользователя обязательно");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BusinessException("Email обязателен");
        }
        if (!user.getEmail().contains("@")) {
            throw new BusinessException("Некорректный email: " + user.getEmail());
        }
        if (user.getRole() == null) {
            throw new BusinessException("Роль пользователя обязательна");
        }
    }
}