package com.netfliz.worker.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingResponse {
    private List<VideoResponse> trendingToday;
    private List<VideoResponse> trendingThisWeek;
    private List<VideoResponse> topRated;
    private List<VideoResponse> mostViewed;
}
