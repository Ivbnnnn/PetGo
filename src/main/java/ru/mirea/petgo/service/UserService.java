package ru.mirea.petgo.service;

import ru.mirea.petgo.exception.BusinessException;
import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.exception.EntityNotFoundException;
import ru.mirea.petgo.exception.ValidationException;
import ru.mirea.petgo.model.User;
import ru.mirea.petgo.repository.UserRepository;
import ru.mirea.petgo.repository.WalkRequestRepository;
import ru.mirea.petgo.repository.PetRepository;
import ru.mirea.petgo.util.Validators;
import java.sql.SQLException;
import java.util.List;

public class UserService {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final WalkRequestRepository walkRequestRepository;

    public UserService(UserRepository userRepository, PetRepository petRepository,
            WalkRequestRepository walkRequestRepository) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.walkRequestRepository = walkRequestRepository;
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

    public void delete(int id) throws EntityNotFoundException, BusinessException, DatabaseException {
        findById(id);
        try {
            int pets = petRepository.countByOwnerId(id);
            int requests = walkRequestRepository.countByOwnerId(id) + walkRequestRepository.countByWalkerId(id);
            if (pets > 0 || requests > 0) {
                throw new BusinessException("Нельзя удалить пользователя: питомцев — " + pets
                        + ", заявок — " + requests + ". Сначала удалите связанные записи.");
            }
            if (!userRepository.deleteById(id)) {
                throw new EntityNotFoundException("Пользователь с id=" + id + " не найден");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось удалить пользователя", e);
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
        user.setName(Validators.requireText("name", "Имя", user.getName(), 100));
        String email = Validators.requireText("email", "Email", user.getEmail(), 100).toLowerCase();
        Validators.email("email", email);
        user.setEmail(email);
        Validators.optionalPhone("phone", user.getPhone());
        Validators.optionalText("address", "Адрес", user.getAddress(), 200);
        if (user.getRole() == null) {
            throw new ValidationException("role", "Выберите роль");
        }
    }
}