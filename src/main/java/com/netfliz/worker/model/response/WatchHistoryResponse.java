package com.netfliz.worker.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchHistoryResponse {
    private Long id;
    private Long videoId;
    private String videoTitle;
    private String thumbnailUrl;
    private LocalDateTime watchedAt;
    private Integer watchDuration; // seconds watched
    private String deviceType;
    private String quality;
}
