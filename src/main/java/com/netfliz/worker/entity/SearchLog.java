package com.netfliz.worker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    private String query;

    @Column(name = "results_count")
    private Integer resultsCount;

    private LocalDateTime timestamp;
}
