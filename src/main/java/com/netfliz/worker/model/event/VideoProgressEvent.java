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
public class VideoProgressEvent {
    private Long userId;
    private Long videoId;
    private Integer currentPosition; // Giây
    private Integer totalDuration;
    private Integer percentComplete;
    private LocalDateTime timestamp;
    private Boolean completed;
}