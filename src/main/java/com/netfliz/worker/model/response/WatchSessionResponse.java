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
public class WatchSessionResponse {
    private String sessionId;
    private Long videoId;
    private String videoTitle;
    private String videoUrl;
    private Integer duration;
    private Integer resumePosition; // Vị trí tiếp tục xem (seconds)
    private String quality;
    private LocalDateTime sessionStartTime;
    private String streamingToken; // Token để authenticate video stream
}
