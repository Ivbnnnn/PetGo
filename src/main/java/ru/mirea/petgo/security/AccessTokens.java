package ru.mirea.petgo.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class AccessTokens {
    private final JwtEncoder encoder;
    private final String issuer;

    public AccessTokens(JwtEncoder encoder, @Value("${app.jwt.issuer}") String issuer) {
        this.encoder = encoder;
        this.issuer = issuer;
    }

    public String issue(UUID userId, UUID sessionId, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("sid", sessionId.toString())
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
