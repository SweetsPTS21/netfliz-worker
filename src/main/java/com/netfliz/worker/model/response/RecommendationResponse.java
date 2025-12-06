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
public class RecommendationResponse {
    private List<VideoResponse> personalizedRecommendations;
    private List<VideoResponse> trendingVideos;
    private List<VideoResponse> continueWatching;
    private List<VideoResponse> newReleases;
    private List<VideoResponse> becauseYouWatched; // Based on last watched
}
