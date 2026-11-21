package ru.mirea.petgo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "walk_requests")
public class WalkRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String externalId; // Код заявки

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "walker_id")
    private User walker;

    @ManyToMany
    @JoinTable(name = "walk_request_tags",
            joinColumns = @JoinColumn(name = "walk_request_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private String address;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private Double price;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    @Enumerated(EnumType.STRING)
    private RequestPriority priority; // Вычисляемое поле

    private Boolean isArchived = false;

    @Version
    private Long version;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
