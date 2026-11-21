package ru.mirea.petgo.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import ru.mirea.petgo.domain.User;
import ru.mirea.petgo.repository.RefreshSessionRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class AccountAuthentication implements Converter<Jwt, JwtAuthenticationToken> {
    private final RefreshSessionRepository sessions;

    public AccountAuthentication(RefreshSessionRepository sessions) {
        this.sessions = sessions;
    }

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        UUID userId = parseId(jwt.getSubject());
        UUID sessionId = parseId(jwt.getClaimAsString("sid"));
        User user = sessions.findByIdAndUser_Id(sessionId, userId)
                .filter(session -> session.isValidFor(session.getUser(), Instant.now()))
                .orElseThrow(this::invalidToken)
                .getUser();
        List<SimpleGrantedAuthority> authorities = user.isPasswordChangeRequired()
                ? List.of(new SimpleGrantedAuthority("PASSWORD_CHANGE_REQUIRED"))
                : List.of(new SimpleGrantedAuthority("ROLE_" + user.getAccessLevel()),
                          new SimpleGrantedAuthority("ROLE_" + user.getRole()));
        return new JwtAuthenticationToken(jwt, authorities, userId.toString());
    }

    private UUID parseId(String value) {
        if (value == null) {
            throw invalidToken();
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw invalidToken();
        }
    }

    private InvalidBearerTokenException invalidToken() {
        return new InvalidBearerTokenException("Сессия недействительна");
    }
}
