package ru.mirea.petgo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.mirea.petgo.domain.User;
import ru.mirea.petgo.dto.AuthDtos.ChangePassword;
import ru.mirea.petgo.dto.AuthDtos.Login;
import ru.mirea.petgo.repository.RefreshSessionRepository;
import ru.mirea.petgo.repository.UserRepository;
import ru.mirea.petgo.security.AccessTokens;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private UserRepository users;
    private RefreshSessionRepository sessions;
    private PasswordEncoder passwords;
    private AuditService audit;
    private AuthService service;
    private User user;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        sessions = mock(RefreshSessionRepository.class);
        passwords = mock(PasswordEncoder.class);
        audit = mock(AuditService.class);
        AccessTokens tokens = mock(AccessTokens.class);
        when(passwords.encode(anyString())).thenReturn("dummy-bcrypt-hash");
        service = new AuthService(users, sessions, passwords, tokens, audit, 15, 14);
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("owner@example.com");
        user.setEnabled(true);
        user.setPasswordHash("stored-bcrypt-hash");
    }

    @Test
    void fiveFailedLoginsLockAccountForFifteenMinutes() {
        when(users.findForLogin("owner@example.com")).thenReturn(Optional.of(user));
        when(passwords.matches("WrongPassword123", "stored-bcrypt-hash")).thenReturn(false);

        for (int i = 0; i < 5; i++) {
            assertThrows(BadCredentialsException.class,
                    () -> service.login(new Login(" OWNER@example.com ", "WrongPassword123")));
        }

        assertEquals(5, user.getFailedLoginAttempts());
        assertNotNull(user.getLockedUntil());
        assertTrue(user.getLockedUntil().isAfter(Instant.now()));
        verify(audit, times(5)).recordAs(isNull(), eq("LOGIN_FAILED"), any(), eq(user.getId()), anyString());
    }

    @Test
    void lockedAccountCannotLoginEvenWithCorrectPassword() {
        user.setLockedUntil(Instant.now().plusSeconds(600));
        when(users.findForLogin("owner@example.com")).thenReturn(Optional.of(user));
        when(passwords.matches("CorrectPassword123", "stored-bcrypt-hash")).thenReturn(true);

        assertThrows(BadCredentialsException.class,
                () -> service.login(new Login("owner@example.com", "CorrectPassword123")));
        verify(sessions, never()).save(any());
    }

    @Test
    void passwordChangeInvalidatesPreviousCredentialVersion() {
        when(users.findForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(passwords.matches("CurrentPassword123", "stored-bcrypt-hash")).thenReturn(true);
        UUID previousVersion = user.getCredentialVersion();

        service.changePassword(user.getId(), new ChangePassword("CurrentPassword123", "NewPassword123"));

        assertNotEquals(previousVersion, user.getCredentialVersion());
        assertFalse(user.isPasswordChangeRequired());
        assertEquals("dummy-bcrypt-hash", user.getPasswordHash());
        verify(audit).recordAs(eq(user.getId()), eq("PASSWORD_CHANGED"), any(), eq(user.getId()), anyString());
    }

    @Test
    void rejectsPasswordShorterThanTwelveCharacters() {
        when(users.findForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(passwords.matches("CurrentPassword123", "stored-bcrypt-hash")).thenReturn(true);

        assertThrows(ru.mirea.petgo.error.BadRequestException.class,
                () -> service.changePassword(user.getId(), new ChangePassword("CurrentPassword123", "short")));
    }
}
