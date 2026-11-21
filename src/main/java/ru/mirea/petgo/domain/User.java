package ru.mirea.petgo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 160)
    private String username;

    @Column(nullable = false, length = 254)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.OWNER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccessLevel accessLevel = AccessLevel.VIEWER;

    private boolean enabled = true;

    @Column(length = 60)
    private String passwordHash;

    private boolean passwordChangeRequired = true;
    private UUID credentialVersion = UUID.randomUUID();
    private int failedLoginAttempts;
    private Instant lockedUntil;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private long version;

    public void replacePassword(String hash, boolean temporary) {
        passwordHash = hash;
        passwordChangeRequired = temporary;
        credentialVersion = UUID.randomUUID();
        failedLoginAttempts = 0;
        lockedUntil = null;
    }

    public void changeEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            credentialVersion = UUID.randomUUID();
            failedLoginAttempts = 0;
            lockedUntil = null;
        }
    }
}
