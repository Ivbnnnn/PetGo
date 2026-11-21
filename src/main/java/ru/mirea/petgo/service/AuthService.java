package ru.mirea.petgo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.petgo.domain.AuditEvent.EntityType;
import ru.mirea.petgo.domain.RefreshSession;
import ru.mirea.petgo.domain.User;
import ru.mirea.petgo.dto.AuthDtos.ChangePassword;
import ru.mirea.petgo.dto.AuthDtos.Login;
import ru.mirea.petgo.dto.AuthDtos.Tokens;
import ru.mirea.petgo.dto.UserDto;
import ru.mirea.petgo.error.BadRequestException;
import ru.mirea.petgo.repository.RefreshSessionRepository;
import ru.mirea.petgo.repository.UserRepository;
import ru.mirea.petgo.security.AccessTokens;
import ru.mirea.petgo.security.PasswordPolicy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_TIME = Duration.ofMinutes(15);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final RefreshSessionRepository sessions;
    private final PasswordEncoder passwords;
    private final AccessTokens tokens;
    private final AuditService audit;
    private final Duration accessLifetime;
    private final Duration refreshLifetime;
    private final String dummyHash;

    public AuthService(UserRepository users, RefreshSessionRepository sessions, PasswordEncoder passwords,
                       AccessTokens tokens, AuditService audit, @Value("${app.jwt.access-minutes}") long accessMinutes,
                       @Value("${app.jwt.refresh-days}") long refreshDays) {
        this.users = users;
        this.sessions = sessions;
        this.passwords = passwords;
        this.tokens = tokens;
        this.audit = audit;
        if (accessMinutes < 1 || refreshDays < 1) {
            throw new IllegalArgumentException("Сроки действия токенов должны быть положительными");
        }
        this.accessLifetime = Duration.ofMinutes(accessMinutes);
        this.refreshLifetime = Duration.ofDays(refreshDays);
        this.dummyHash = passwords.encode(newRefreshToken());
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public Tokens login(Login input) {
        Instant now = Instant.now();
        User user = users.findForLogin(normalizeEmail(input.email())).orElse(null);
        String hash = user == null ? null : user.getPasswordHash();
        boolean matches = PasswordPolicy.fitsBcrypt(input.password())
                && passwords.matches(input.password(), hash == null ? dummyHash : hash);
        if (user == null || !user.isEnabled() || hash == null
                || user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw loginFailed(user);
        }
        if (!matches) {
            int attempts = user.getLockedUntil() == null ? user.getFailedLoginAttempts() + 1 : 1;
            user.setFailedLoginAttempts(attempts);
            user.setLockedUntil(attempts >= MAX_FAILED_ATTEMPTS ? now.plus(LOCK_TIME) : null);
            throw loginFailed(user);
        }
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        sessions.deleteExpired(now);
        RefreshSession session = new RefreshSession();
        session.setUser(user);
        session.setCredentialVersion(user.getCredentialVersion());
        session.setExpiresAt(now.plus(refreshLifetime));
        audit.recordAs(user.getId(), "LOGIN_SUCCEEDED", EntityType.USER, user.getId(), "Вход выполнен");
        return issue(session, now);
    }

    @Transactional
    public Tokens refresh(String token) {
        Instant now = Instant.now();
        RefreshSession session = sessions.findByTokenHash(digest(token)).orElseThrow(this::invalidCredentials);
        if (!session.isValidFor(session.getUser(), now)) {
            throw invalidCredentials();
        }
        return issue(session, now);
    }

    @Transactional
    public void logout(String token) {
        sessions.findByTokenHash(digest(token)).ifPresent(sessions::delete);
    }

    @Transactional(readOnly = true)
    public UserDto me(UUID userId) {
        return users.findById(userId).map(UserDto::from).orElseThrow(this::invalidCredentials);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePassword input) {
        User user = users.findForUpdate(userId).orElseThrow(this::invalidCredentials);
        if (!user.isEnabled() || !PasswordPolicy.fitsBcrypt(input.currentPassword())
                || !passwords.matches(input.currentPassword(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        PasswordPolicy.validate(input.newPassword());
        if (input.currentPassword().equals(input.newPassword())) {
            throw new BadRequestException("Новый пароль должен отличаться от текущего");
        }
        user.replacePassword(passwords.encode(input.newPassword()), false);
        audit.recordAs(userId, "PASSWORD_CHANGED", EntityType.USER, userId, "Пользователь сменил пароль");
    }

    public static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private Tokens issue(RefreshSession session, Instant now) {
        String refresh = newRefreshToken();
        session.setTokenHash(digest(refresh));
        sessions.save(session);
        Instant accessExpiresAt = now.plus(accessLifetime);
        if (accessExpiresAt.isAfter(session.getExpiresAt())) {
            accessExpiresAt = session.getExpiresAt();
        }
        User user = session.getUser();
        String access = tokens.issue(user.getId(), session.getId(), now, accessExpiresAt);
        return new Tokens(access, accessExpiresAt, refresh, session.getExpiresAt(), UserDto.from(user));
    }

    private static String newRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String digest(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private BadCredentialsException invalidCredentials() {
        return new BadCredentialsException("Неверные учётные данные или сессия недоступна");
    }

    private BadCredentialsException loginFailed(User user) {
        audit.recordAs(null, "LOGIN_FAILED", EntityType.USER, user == null ? null : user.getId(),
                "Попытка входа отклонена");
        return invalidCredentials();
    }
}
