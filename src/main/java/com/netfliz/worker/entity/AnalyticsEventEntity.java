package com.netfliz.worker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "analytics_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_id")
    private String sessionId;

    private String page;

    private String action;

    @Column(length = 2000)
    private String properties;

    private LocalDateTime timestamp;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
