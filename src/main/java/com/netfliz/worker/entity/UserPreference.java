package com.netfliz.worker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    private String genre;

    private Double score;

    @Column(name = "interaction_count")
    private Integer interactionCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}
