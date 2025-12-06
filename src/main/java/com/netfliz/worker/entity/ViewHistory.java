package com.netfliz.worker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "view_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "video_id")
    private Long videoId;

    @Column(name = "video_title")
    private String videoTitle;

    @Column(name = "view_start_time")
    private LocalDateTime viewStartTime;

    @Column(name = "watch_duration")
    private Integer watchDuration;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "ip_address")
    private String ipAddress;

    private String quality;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
