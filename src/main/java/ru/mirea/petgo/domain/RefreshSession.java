package ru.mirea.petgo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_sessions")
@Getter
@Setter
public class RefreshSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private UUID credentialVersion;

    @Column(nullable = false)
    private Instant expiresAt;

    public boolean isValidFor(User account, Instant now) {
        return account.isEnabled() && expiresAt.isAfter(now)
                && credentialVersion.equals(account.getCredentialVersion());
    }
}
