package com.netfliz.worker.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoStatsResponse {
    private Long totalViews;
    private Long uniqueViewers;
    private Long totalLikes;
    private Long totalComments;
    private Long totalShares;
    private Double averageRating;
    private Integer totalRatings;
    private Double completionRate; // % người xem hết phim
}
