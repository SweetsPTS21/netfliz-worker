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
public class RecommendationEvent {
    private Long userId;
    private Long videoId;
    private String recommendationType; // TRENDING, PERSONALIZED, SIMILAR, NEW_RELEASE
    private Double score;
    private LocalDateTime generatedAt;
}
