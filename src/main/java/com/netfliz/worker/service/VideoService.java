package com.netfliz.worker.service;

import com.netfliz.worker.entity.*;
import com.netfliz.worker.model.event.UserActivityEvent;
import com.netfliz.worker.model.event.VideoViewEvent;
import com.netfliz.worker.model.request.VideoFilterRequest;
import com.netfliz.worker.model.request.VideoSearchRequest;
import com.netfliz.worker.model.response.*;
import com.netfliz.worker.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final ViewHistoryRepository viewHistoryRepository;
    private final WatchProgressRepository watchProgressRepository;
    private final WatchlistRepository watchlistRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    /**
     * Tạo watch session khi user bắt đầu xem video
     */
    @Transactional
    public WatchSessionResponse createWatchSession(Long userId, Long videoId) {
        try {
            log.info("Creating watch session - userId: {}, videoId: {}", userId, videoId);

            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

            // Check if user has permission to watch (subscription)
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            if (!canUserWatchVideo(user, video)) {
                throw new RuntimeException("User does not have permission to watch this video");
            }

            // Get resume position from watch progress
            Integer resumePosition = watchProgressRepository
                    .findByUserIdAndVideoId(userId, videoId)
                    .map(WatchProgress::getCurrentPosition)
                    .orElse(0);

            String sessionId = UUID.randomUUID().toString();
            String streamingToken = generateStreamingToken(userId, videoId, sessionId);

            log.info("✓ Watch session created - sessionId: {}, resumeFrom: {}s",
                    sessionId, resumePosition);

            return WatchSessionResponse.builder()
                    .sessionId(sessionId)
                    .videoId(video.getId())
                    .videoTitle(video.getTitle())
                    .videoUrl("/stream/" + videoId) // Actual streaming URL
                    .duration(video.getDuration())
                    .resumePosition(resumePosition)
                    .quality("1080p") // Default quality
                    .sessionStartTime(LocalDateTime.now())
                    .streamingToken(streamingToken)
                    .build();

        } catch (Exception e) {
            log.error("Failed to create watch session", e);
            throw e;
        }
    }

    /**
     * Lấy video detail với các thông tin bổ sung
     */
    @Cacheable(value = "videoDetail", key = "#videoId + '_' + #userId")
    public VideoDetailResponse getVideoDetail(Long videoId, Long userId) {
        try {
            log.debug("Fetching video detail - videoId: {}, userId: {}", videoId, userId);

            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

            // Get similar videos
            List<VideoResponse> similarVideos = videoRepository
                    .findSimilarVideos(video.getGenre(), video.getDirector(), videoId)
                    .stream()
                    .limit(6)
                    .map(v -> mapToVideoResponse(v, userId))
                    .collect(Collectors.toList());

            // Get top comments
            List<CommentResponse> topComments = commentRepository
                    .findByVideoIdOrderByLikeCountDescCreatedAtDesc(videoId, PageRequest.of(0, 5))
                    .stream()
                    .map(this::mapToCommentResponse)
                    .collect(Collectors.toList());

            // Get stats
            VideoStatsResponse stats = getVideoStats(videoId);

            // Check permission
            User user = userRepository.findById(userId).orElse(null);
            boolean canWatch = user != null && canUserWatchVideo(user, video);

            return VideoDetailResponse.builder()
                    .video(mapToVideoResponse(video, userId))
                    .similarVideos(similarVideos)
                    .topComments(topComments)
                    .stats(stats)
                    .canWatch(canWatch)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get video detail", e);
            throw e;
        }
    }

    /**
     * Search videos
     */
    public SearchResponse search(VideoSearchRequest request) {
        try {
            long startTime = System.currentTimeMillis();

            log.info("Searching videos - query: '{}', genre: {}",
                    request.getQuery(), request.getGenre());

            Pageable pageable = PageRequest.of(
                    request.getPage(),
                    request.getSize()
            );

            Page<Video> videoPage;

            if (request.getGenre() != null) {
                videoPage = videoRepository.searchByQueryAndGenre(
                        request.getQuery(),
                        request.getGenre(),
                        pageable
                );
            } else {
                videoPage = videoRepository.searchByQuery(request.getQuery(), pageable);
            }

            List<VideoResponse> videos = videoPage.getContent()
                    .stream()
                    .map(v -> mapToVideoResponse(v, null))
                    .collect(Collectors.toList());

            long searchTime = System.currentTimeMillis() - startTime;

            return SearchResponse.builder()
                    .videos(videos)
                    .totalResults((int) videoPage.getTotalElements())
                    .currentPage(request.getPage())
                    .totalPages(videoPage.getTotalPages())
                    .query(request.getQuery())
                    .searchTime(searchTime)
                    .build();

        } catch (Exception e) {
            log.error("Failed to search videos", e);
            throw e;
        }
    }

    /**
     * Filter videos
     */
    public PaginatedResponse<VideoResponse> filterVideos(VideoFilterRequest request, Long userId) {
        try {
            log.debug("Filtering videos - genre: {}, director: {}",
                    request.getGenre(), request.getDirector());

            Sort sort = Sort.by(
                    request.getSortOrder().equalsIgnoreCase("ASC")
                            ? Sort.Direction.ASC
                            : Sort.Direction.DESC,
                    request.getSortBy()
            );

            Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

            Page<Video> videoPage = videoRepository.filterVideos(
                    request.getGenre(),
                    request.getDirector(),
                    request.getReleaseYear(),
                    request.getMinRating(),
                    pageable
            );

            List<VideoResponse> videos = videoPage.getContent()
                    .stream()
                    .map(v -> mapToVideoResponse(v, userId))
                    .collect(Collectors.toList());

            return PaginatedResponse.<VideoResponse>builder()
                    .content(videos)
                    .currentPage(request.getPage())
                    .pageSize(request.getSize())
                    .totalElements(videoPage.getTotalElements())
                    .totalPages(videoPage.getTotalPages())
                    .hasNext(videoPage.hasNext())
                    .hasPrevious(videoPage.hasPrevious())
                    .build();

        } catch (Exception e) {
            log.error("Failed to filter videos", e);
            throw e;
        }
    }

    /**
     * Get recommendations for user
     */
    public RecommendationResponse getRecommendations(Long userId) {
        try {
            log.debug("Getting recommendations for userId: {}", userId);

            // Personalized recommendations
            List<VideoResponse> personalized = videoRepository
                    .findPersonalizedRecommendations(userId, PageRequest.of(0, 10))
                    .stream()
                    .map(v -> mapToVideoResponse(v, userId))
                    .collect(Collectors.toList());

            // Trending videos
            List<VideoResponse> trending = videoRepository
                    .findTrendingVideos(LocalDateTime.now().minusDays(7), 10)
                    .stream()
                    .map(v -> mapToVideoResponse(v, userId))
                    .collect(Collectors.toList());

            // Continue watching
            List<VideoResponse> continueWatching = getContinueWatching(userId)
                    .stream()
                    .map(cw -> {
                        Video video = videoRepository.findById(cw.getVideoId()).orElse(null);
                        return video != null ? mapToVideoResponse(video, userId) : null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // New releases
            List<VideoResponse> newReleases = videoRepository
                    .findNewReleases(LocalDateTime.now().minusMonths(1), PageRequest.of(0, 10))
                    .stream()
                    .map(v -> mapToVideoResponse(v, userId))
                    .collect(Collectors.toList());

            return RecommendationResponse.builder()
                    .personalizedRecommendations(personalized)
                    .trendingVideos(trending)
                    .continueWatching(continueWatching)
                    .newReleases(newReleases)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get recommendations", e);
            throw e;
        }
    }

    /**
     * Get continue watching list
     */
    public List<ContinueWatchingResponse> getContinueWatching(Long userId) {
        try {
            return watchProgressRepository.findByUserIdAndPercentCompleteLessThan(userId, 95)
                    .stream()
                    .map(wp -> {
                        Video video = videoRepository.findById(wp.getVideoId()).orElse(null);
                        if (video == null) return null;

                        return ContinueWatchingResponse.builder()
                                .videoId(video.getId())
                                .title(video.getTitle())
                                .thumbnailUrl("/thumbnails/" + video.getId())
                                .currentPosition(wp.getCurrentPosition())
                                .duration(video.getDuration() * 60) // convert to seconds
                                .percentComplete(wp.getPercentComplete())
                                .lastWatched(wp.getLastUpdated())
                                .build();
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get continue watching", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get watchlist
     */
    public WatchlistResponse getWatchlist(Long userId) {
        try {
            List<VideoResponse> videos = watchlistRepository
                    .findByUserIdOrderByAddedAtDesc(userId)
                    .stream()
                    .map(w -> {
                        Video video = videoRepository.findById(w.getVideoId()).orElse(null);
                        return video != null ? mapToVideoResponse(video, userId) : null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return WatchlistResponse.builder()
                    .videos(videos)
                    .totalCount(videos.size())
                    .build();

        } catch (Exception e) {
            log.error("Failed to get watchlist", e);
            throw e;
        }
    }

    /**
     * Get watch history
     */
    public List<WatchHistoryResponse> getWatchHistory(Long userId, int limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit);

            return viewHistoryRepository.findByUserIdOrderByViewStartTimeDesc(userId, pageable)
                    .stream()
                    .map(vh -> WatchHistoryResponse.builder()
                            .id(vh.getId())
                            .videoId(vh.getVideoId())
                            .videoTitle(vh.getVideoTitle())
                            .thumbnailUrl("/thumbnails/" + vh.getVideoId())
                            .watchedAt(vh.getViewStartTime())
                            .watchDuration(vh.getWatchDuration())
                            .deviceType(vh.getDeviceType())
                            .quality(vh.getQuality())
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get watch history", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get trending videos
     */
    public TrendingResponse getTrending() {
        try {
            LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
            LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);

            List<VideoResponse> trendingToday = videoRepository
                    .findTrendingVideos(oneDayAgo, 10)
                    .stream()
                    .map(v -> mapToVideoResponse(v, null))
                    .collect(Collectors.toList());

            List<VideoResponse> trendingThisWeek = videoRepository
                    .findTrendingVideos(oneWeekAgo, 10)
                    .stream()
                    .map(v -> mapToVideoResponse(v, null))
                    .collect(Collectors.toList());

            List<VideoResponse> topRated = videoRepository
                    .findTopRated(PageRequest.of(0, 10))
                    .stream()
                    .map(v -> mapToVideoResponse(v, null))
                    .collect(Collectors.toList());

            List<VideoResponse> mostViewed = videoRepository
                    .findMostViewed(PageRequest.of(0, 10))
                    .stream()
                    .map(v -> mapToVideoResponse(v, null))
                    .collect(Collectors.toList());

            return TrendingResponse.builder()
                    .trendingToday(trendingToday)
                    .trendingThisWeek(trendingThisWeek)
                    .topRated(topRated)
                    .mostViewed(mostViewed)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get trending videos", e);
            throw e;
        }
    }

    /**
     * Get video stats
     */
    private VideoStatsResponse getVideoStats(Long videoId) {
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null) return null;

        long uniqueViewers = viewHistoryRepository.countDistinctUsersByVideoId(videoId);
        long totalComments = commentRepository.countByVideoIdAndIsDeletedFalse(videoId);

        return VideoStatsResponse.builder()
                .totalViews(video.getViewCount())
                .uniqueViewers(uniqueViewers)
                .totalLikes(video.getLikeCount())
                .totalComments(totalComments)
                .totalShares(video.getShareCount())
                .averageRating(video.getRating())
                .totalRatings(0) // TODO: Implement ratings
                .completionRate(75.0) // TODO: Calculate from watch progress
                .build();
    }

    /**
     * Map Video entity to VideoResponse DTO
     */
    private VideoResponse mapToVideoResponse(Video video, Long userId) {
        Boolean isLiked = false;
        Boolean isInWatchlist = false;
        Integer watchProgress = 0;

        if (userId != null) {
            isLiked = likeRepository.existsByUserIdAndVideoId(userId, video.getId());
            isInWatchlist = watchlistRepository.existsByUserIdAndVideoId(userId, video.getId());
            watchProgress = watchProgressRepository.findByUserIdAndVideoId(userId, video.getId())
                    .map(WatchProgress::getPercentComplete)
                    .orElse(0);
        }

        return VideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .genre(video.getGenre())
                .director(video.getDirector())
                .cast(video.getCast())
                .duration(video.getDuration())
                .rating(video.getRating())
                .releaseDate(video.getReleaseDate())
                .viewCount(video.getViewCount())
                .likeCount(video.getLikeCount())
                .shareCount(video.getShareCount())
                .thumbnailUrl("/thumbnails/" + video.getId())
                .trailerUrl("/trailers/" + video.getId())
                .isLiked(isLiked)
                .isInWatchlist(isInWatchlist)
                .watchProgress(watchProgress)
                .build();
    }

    /**
     * Map Comment entity to CommentResponse DTO
     */
    private CommentResponse mapToCommentResponse(Comment comment) {
        User user = userRepository.findById(comment.getUserId()).orElse(null);

        return CommentResponse.builder()
                .id(comment.getId())
                .userId(comment.getUserId())
                .username(user != null ? user.getUsername() : "Unknown")
                .userAvatar("/avatars/" + comment.getUserId())
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .isLiked(false) // TODO: Check if current user liked
                .createdAt(comment.getCreatedAt())
                .replies(Collections.emptyList()) // TODO: Load replies
                .build();
    }

    // Existing methods from previous implementation
    @Transactional
    @CacheEvict(value = "videos", key = "#videoId")
    public void incrementViewCount(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        video.setViewCount(video.getViewCount() + 1);
        video.setLastViewedAt(LocalDateTime.now());
        videoRepository.save(video);
    }

    @Transactional
    public void saveViewHistory(VideoViewEvent event) {
        ViewHistory history = ViewHistory.builder()
                .userId(event.getUserId())
                .videoId(event.getVideoId())
                .videoTitle(event.getVideoTitle())
                .viewStartTime(event.getViewStartTime())
                .watchDuration(event.getWatchDuration())
                .deviceType(event.getDeviceType())
                .ipAddress(event.getIpAddress())
                .quality(event.getQuality())
                .createdAt(LocalDateTime.now())
                .build();
        viewHistoryRepository.save(history);
    }

    @Transactional
    public void updateWatchProgress(Long userId, Long videoId, Integer currentPosition, Integer percentComplete) {
        WatchProgress progress = watchProgressRepository.findByUserIdAndVideoId(userId, videoId)
                .orElse(WatchProgress.builder()
                        .userId(userId)
                        .videoId(videoId)
                        .createdAt(LocalDateTime.now())
                        .build());

        progress.setCurrentPosition(currentPosition);
        progress.setPercentComplete(percentComplete);
        progress.setLastUpdated(LocalDateTime.now());
        watchProgressRepository.save(progress);
    }

    @Transactional
    @CacheEvict(value = "videos", key = "#videoId")
    public void handleLike(Long userId, Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        video.setLikeCount(video.getLikeCount() + 1);
        videoRepository.save(video);
    }

    @Transactional
    public void handleComment(UserActivityEvent event) {
        // Comment handling logic
    }

    @Transactional
    @CacheEvict(value = "videos", key = "#videoId")
    public void incrementShareCount(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        video.setShareCount(video.getShareCount() + 1);
        videoRepository.save(video);
    }

    @Transactional
    public void addToWatchlist(Long userId, Long videoId) {
        if (watchlistRepository.findByUserIdAndVideoId(userId, videoId).isPresent()) {
            return;
        }

        Watchlist watchlist = Watchlist.builder()
                .userId(userId)
                .videoId(videoId)
                .addedAt(LocalDateTime.now())
                .build();
        watchlistRepository.save(watchlist);
    }

    private boolean canUserWatchVideo(User user, Video video) {
        // Free videos accessible to all
        // Premium videos require subscription
        return user.getIsPremium() || video.getGenre().equals("Free");
    }

    private String generateStreamingToken(Long userId, Long videoId, String sessionId) {
        // Generate JWT or similar token for streaming authentication
        return Base64.getEncoder().encodeToString(
                (userId + ":" + videoId + ":" + sessionId).getBytes()
        );
    }
}