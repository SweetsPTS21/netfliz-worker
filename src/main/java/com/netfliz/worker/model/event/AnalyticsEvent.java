package com.netfliz.worker.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEvent {
    private String eventType;
    private Long userId;
    private String sessionId;
    private String page;
    private String action;
    private Map<String, Object> properties;
    private LocalDateTime timestamp;
}
