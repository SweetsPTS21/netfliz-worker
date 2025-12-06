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
public class VideoResponse {
    private Long id;
    private String title;
    private String description;
    private String genre;
    private String director;
    private String cast;
    private Integer duration; // minutes
    private Double rating;
    private LocalDateTime releaseDate;
    private Long viewCount;
    private Long likeCount;
    private Long shareCount;
    private String thumbnailUrl;
    private String trailerUrl;
    private Boolean isLiked; // User đã like chưa
    private Boolean isInWatchlist; // Có trong watchlist không
    private Integer watchProgress; // Phần trăm đã xem
}
