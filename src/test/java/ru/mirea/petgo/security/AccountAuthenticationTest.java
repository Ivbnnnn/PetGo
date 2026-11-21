package ru.mirea.petgo.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import ru.mirea.petgo.domain.AccessLevel;
import ru.mirea.petgo.domain.RefreshSession;
import ru.mirea.petgo.domain.User;
import ru.mirea.petgo.domain.UserRole;
import ru.mirea.petgo.repository.RefreshSessionRepository;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountAuthenticationTest {
    private RefreshSessionRepository sessions;
    private AccountAuthentication authentication;
    private UUID userId;
    private UUID sessionId;
    private User user;
    private RefreshSession session;

    @BeforeEach
    void setUp() {
        sessions = mock(RefreshSessionRepository.class);
        authentication = new AccountAuthentication(sessions);
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setRole(UserRole.OWNER);
        user.setAccessLevel(AccessLevel.VIEWER);
        user.setEnabled(true);
        user.setPasswordChangeRequired(false);   // ← ИСПРАВЛЕНИЕ: сбрасываем флаг
        session = new RefreshSession();
        session.setId(sessionId);
        session.setUser(user);
        session.setCredentialVersion(user.getCredentialVersion());
        session.setExpiresAt(Instant.now().plusSeconds(300));
        when(sessions.findByIdAndUser_Id(sessionId, userId)).thenReturn(Optional.of(session));
    }

    @Test
    void grantsOnlyCurrentUserRoleAndAccessLevel() {
        JwtAuthenticationToken result = authentication.convert(jwt());
        Set<String> authorities = result.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

        assertEquals(userId.toString(), result.getName());
        assertEquals(Set.of("ROLE_OWNER", "ROLE_VIEWER"), authorities);
    }

    @Test
    void temporaryPasswordAllowsOnlyPasswordChangeAuthority() {
        user.setPasswordChangeRequired(true);

        JwtAuthenticationToken result = authentication.convert(jwt());

        assertEquals(Set.of("PASSWORD_CHANGE_REQUIRED"), result.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toSet()));
    }

    @Test
    void rejectsExpiredOrRevokedSession() {
        session.setExpiresAt(Instant.now().minusSeconds(1));

        assertThrows(InvalidBearerTokenException.class, () -> authentication.convert(jwt()));
    }

    @Test
    void rejectsUnknownSession() {
        when(sessions.findByIdAndUser_Id(sessionId, userId)).thenReturn(Optional.empty());

        assertThrows(InvalidBearerTokenException.class, () -> authentication.convert(jwt()));
    }

    private Jwt jwt() {
        Instant now = Instant.now();
        return Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .issuer("petgo")
                .subject(userId.toString())
                .claim("sid", sessionId.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .build();
    }
}