package com.netfliz.worker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_notification_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "push_enabled")
    private boolean pushEnabled = true;

    @Column(name = "email_enabled")
    private boolean emailEnabled = true;

    @Column(name = "sms_enabled")
    private boolean smsEnabled = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
