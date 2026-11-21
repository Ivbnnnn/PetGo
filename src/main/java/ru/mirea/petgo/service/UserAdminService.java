package ru.mirea.petgo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.petgo.domain.AuditEvent.EntityType;
import ru.mirea.petgo.domain.User;
import ru.mirea.petgo.dto.UserAdminDtos.Create;
import ru.mirea.petgo.dto.UserAdminDtos.Update;
import ru.mirea.petgo.dto.UserDto;
import ru.mirea.petgo.error.ConflictException;
import ru.mirea.petgo.error.ResourceNotFoundException;
import ru.mirea.petgo.filter.PageSort;
import ru.mirea.petgo.repository.UserRepository;
import ru.mirea.petgo.security.PasswordPolicy;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAdminService {
    private static final Set<String> SORT_FIELDS =
            Set.of("id", "email", "username", "role", "accessLevel", "enabled", "createdAt");

    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public Page<UserDto> list(Pageable page) {
        return users.findAll(PageSort.apply(page, Sort.by("email"), SORT_FIELDS)).map(UserDto::from);
    }

    @Transactional(readOnly = true)
    public UserDto get(UUID id) {
        return users.findById(id).map(UserDto::from).orElseThrow(UserAdminService::notFound);
    }

    @Transactional
    public UserDto create(Create input) {
        String email = AuthService.normalizeEmail(input.email());
        requireAvailable(email);
        User user = new User();
        user.setEmail(email);
        user.setUsername(input.username().strip());
        user.setRole(input.role());
        user.setAccessLevel(input.accessLevel());
        setTemporaryPassword(user, input.temporaryPassword());
        users.saveAndFlush(user);
        audit.record("USER_CREATED", EntityType.USER, user.getId(), "Создана учётная запись");
        return UserDto.from(user);
    }

    @Transactional
    public UserDto update(UUID id, Update input) {
        User user = forUpdate(id);
        if (user.getVersion() != input.version()) {
            throw new ConflictException("Учётная запись уже изменена");
        }
        String email = AuthService.normalizeEmail(input.email());
        if (!user.getEmail().equals(email)) {
            requireAvailable(email);
            user.setEmail(email);
        }
        user.setUsername(input.username().strip());
        user.setRole(input.role());
        user.setAccessLevel(input.accessLevel());
        user.changeEnabled(input.enabled());
        users.flush();
        audit.record("USER_UPDATED", EntityType.USER, id, "Изменены данные, права или активность учётной записи");
        return UserDto.from(user);
    }

    @Transactional
    public UserDto setEnabled(UUID id, boolean enabled) {
        User user = forUpdate(id);
        if (user.isEnabled() != enabled) {
            user.changeEnabled(enabled);
            users.flush();
            audit.record(enabled ? "USER_UNBLOCKED" : "USER_BLOCKED", EntityType.USER, id,
                    enabled ? "Учётная запись разблокирована" : "Учётная запись заблокирована");
        }
        return UserDto.from(user);
    }

    @Transactional
    public void resetPassword(UUID id, String temporaryPassword) {
        setTemporaryPassword(forUpdate(id), temporaryPassword);
        audit.record("PASSWORD_RESET", EntityType.USER, id, "Назначен временный пароль");
    }

    private void setTemporaryPassword(User user, String password) {
        PasswordPolicy.validate(password);
        user.replacePassword(passwords.encode(password), true);
    }

    private User forUpdate(UUID id) {
        return users.findForUpdate(id).orElseThrow(UserAdminService::notFound);
    }

    private void requireAvailable(String email) {
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Учётная запись с таким email уже существует");
        }
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Учётная запись не найдена");
    }
}
