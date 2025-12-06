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
public class UserActivityEvent {
    private String eventId;
    private Long userId;
    private String activityType; // SEARCH, LIKE, COMMENT, SHARE, ADD_TO_WATCHLIST
    private Long targetId; // ID của video/comment được tương tác
    private String metadata; // JSON string chứa thông tin bổ sung
    private LocalDateTime timestamp;
}
