
package ru.mirea.petgo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "walk_history")
public class WalkHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "walk_request_id",
            nullable = false,
            unique = true
    )
    private WalkRequest walkRequest;

    @Column(name = "actual_duration")
    private Integer actualDuration;

    @Column(columnDefinition = "TEXT")
    private String route;

    @Column(name = "owner_review", columnDefinition = "TEXT")
    private String ownerReview;

    @Column(name = "walker_review", columnDefinition = "TEXT")
    private String walkerReview;

    private Integer rating;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;
}
