package com.netfliz.worker.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String notificationId;
    private Long userId;
    private String type; // NEW_EPISODE, RECOMMENDATION, PAYMENT_DUE, SYSTEM
    private String title;
    private String message;
    private String deepLink; // Link đến nội dung liên quan
    private LocalDateTime createdAt;
    private Boolean sent;
}