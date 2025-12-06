package com.netfliz.worker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "watch_progress")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "video_id")
    private Long videoId;

    @Column(name = "current_position")
    private Integer currentPosition;

    @Column(name = "percent_complete")
    private Integer percentComplete;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}
