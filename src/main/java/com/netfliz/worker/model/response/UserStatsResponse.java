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
public class UserStatsResponse {
    private Integer videosWatched;
    private Integer totalWatchTime; // minutes
    private Integer videosLiked;
    private Integer commentsPosted;
    private Integer watchlistCount;
    private String favoriteGenre;
    private LocalDateTime memberSince;
}
