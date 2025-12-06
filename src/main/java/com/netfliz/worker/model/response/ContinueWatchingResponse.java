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
public class ContinueWatchingResponse {
    private Long videoId;
    private String title;
    private String thumbnailUrl;
    private Integer currentPosition; // seconds
    private Integer duration; // seconds
    private Integer percentComplete;
    private LocalDateTime lastWatched;
}
