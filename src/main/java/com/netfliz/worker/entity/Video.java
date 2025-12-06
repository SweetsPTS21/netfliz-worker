package com.netfliz.worker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

// ==================== VIDEO ====================
@Entity
@Table(name = "videos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String genre;
    private String director;
    private String cast;
    private Integer duration; // minutes
    private Double rating;
    private LocalDateTime releaseDate;

    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Column(name = "like_count")
    private Long likeCount = 0L;

    @Column(name = "share_count")
    private Long shareCount = 0L;

    @Column(name = "last_viewed_at")
    private LocalDateTime lastViewedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
