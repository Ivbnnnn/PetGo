package ru.mirea.petgo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.mirea.petgo.domain.AccessLevel;
import ru.mirea.petgo.domain.User;
import ru.mirea.petgo.repository.UserRepository;
import ru.mirea.petgo.security.PasswordPolicy;
import ru.mirea.petgo.service.AuthService;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final String email;
    private final String username;
    private final String password;

    public AdminBootstrap(UserRepository users, PasswordEncoder passwords,
                          @Value("${app.bootstrap.email}") String email,
                          @Value("${app.bootstrap.name}") String username,
                          @Value("${app.bootstrap.password}") String password) {
        this.users = users;
        this.passwords = passwords;
        this.email = email;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) {
            return;
        }
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("Укажите BOOTSTRAP_ADMIN_EMAIL и BOOTSTRAP_ADMIN_PASSWORD вместе");
        }
        String login = AuthService.normalizeEmail(email);
        if (users.existsByEmailIgnoreCase(login)) {
            return;
        }
        PasswordPolicy.validate(password);
        User admin = new User();
        admin.setEmail(login);
        admin.setUsername(username.strip());
        admin.setAccessLevel(AccessLevel.ADMIN);
        admin.replacePassword(passwords.encode(password), true);
        users.save(admin);
    }
}
