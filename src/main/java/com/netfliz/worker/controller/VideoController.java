package com.netfliz.worker.controller;

import com.netfliz.worker.model.event.AnalyticsEvent;
import com.netfliz.worker.model.event.UserActivityEvent;
import com.netfliz.worker.model.event.VideoProgressEvent;
import com.netfliz.worker.model.event.VideoViewEvent;
import com.netfliz.worker.model.request.*;
import com.netfliz.worker.model.response.*;
import com.netfliz.worker.service.KafkaProducerService;
import com.netfliz.worker.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@Slf4j
public class VideoController {

    private final VideoService videoService;
    private final KafkaProducerService kafkaProducer;

    /**
     * GET /api/videos/{videoId}
     * Lấy chi tiết video
     */
    @GetMapping("/{videoId}")
    public ResponseEntity<ApiResponse<VideoDetailResponse>> getVideoDetail(
            @PathVariable Long videoId,
            @RequestHeader("User-Id") Long userId) {

        try {
            log.info("Getting video detail - videoId: {}, userId: {}", videoId, userId);

            VideoDetailResponse response = videoService.getVideoDetail(videoId, userId);

            // Track analytics
            AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                    .eventType("VIDEO_DETAIL_VIEW")
                    .userId(userId)
                    .page("/videos/" + videoId)
                    .action("view_detail")
                    .timestamp(LocalDateTime.now())
                    .build();
            kafkaProducer.sendAnalyticsEvent(analyticsEvent);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error getting video detail", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get video detail: " + e.getMessage()));
        }
    }

    /**
     * POST /api/videos/{videoId}/start-watching
     * Bắt đầu xem video
     */
    @PostMapping("/{videoId}/start-watching")
    public ResponseEntity<ApiResponse<WatchSessionResponse>> startWatching(
            @PathVariable Long videoId,
            @RequestHeader("User-Id") Long userId,
            @Valid @RequestBody WatchRequest request) {

        try {
            log.info("User {} started watching video {}", userId, videoId);

            // Tạo session xem
            WatchSessionResponse session = videoService.createWatchSession(userId, videoId);

            // Gửi event vào Kafka
            VideoViewEvent event = VideoViewEvent.builder()
                    .userId(userId)
                    .videoId(videoId)
                    .videoTitle(session.getVideoTitle())
                    .viewStartTime(LocalDateTime.now())
                    .deviceType(request.getDeviceType())
                    .ipAddress(request.getIpAddress())
                    .quality(request.getQuality())
                    .build();

            kafkaProducer.sendVideoViewEvent(event);

            return ResponseEntity.ok(ApiResponse.success("Video session started", session));

        } catch (Exception e) {
            log.error("Error starting watch session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to start watching: " + e.getMessage()));
        }
    }

    /**
     * POST /api/videos/{videoId}/progress
     * Cập nhật tiến độ xem
     */
    @PostMapping("/{videoId}/progress")
    public ResponseEntity<ApiResponse<Void>> updateProgress(
            @PathVariable Long videoId,
            @RequestHeader("User-Id") Long userId,
            @Valid @RequestBody VideoProgressRequest request) {

        try {
            log.debug("Updating progress - userId: {}, videoId: {}, position: {}s",
                    userId, videoId, request.getCurrentPosition());

            // Gửi event tiến độ vào Kafka
            VideoProgressEvent event = VideoProgressEvent.builder()
                    .userId(userId)
                    .videoId(videoId)
                    .currentPosition(request.getCurrentPosition())
                    .totalDuration(request.getTotalDuration())
                    .percentComplete(request.getPercentComplete())
                    .timestamp(LocalDateTime.now())
                    .completed(request.getPercentComplete() >= 95)
                    .build();

            kafkaProducer.sendVideoProgressEvent(event);

            return ResponseEntity.ok(ApiResponse.success("Progress updated", null));

        } catch (Exception e) {
            log.error("Error updating progress", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update progress: " + e.getMessage()));
        }
    }

    /**
     * POST /api/videos/{videoId}/like
     * Like video
     */
    @PostMapping("/{videoId}/like")
    public ResponseEntity<ApiResponse<Void>> likeVideo(
            @PathVariable Long videoId,
            @RequestHeader("User-Id") Long userId) {

        try {
            log.info("User {} liked video {}", userId, videoId);

            // Gửi event hoạt động vào Kafka
            UserActivityEvent event = UserActivityEvent.builder()
                    .userId(userId)
                    .activityType("LIKE")
                    .targetId(videoId)
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaProducer.sendUserActivityEvent(event);

            return ResponseEntity.ok(ApiResponse.success("Video liked", null));

        } catch (Exception e) {
            log.error("Error liking video", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to like video: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/videos/{videoId}/like
     * Unlike video
     */
    @DeleteMapping("/{videoId}/like")
    public ResponseEntity<ApiResponse<Void>> unlikeVideo(
            @PathVariable Long videoId,
            @RequestHeader("User-Id") Long userId) {

        try {
            log.info("User {} unliked video {}", userId, videoId);

            UserActivityEvent event = UserActivityEvent.builder()
                    .userId(userId)
                    .activityType("UNLIKE")
                    .targetId(videoId)
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaProducer.sendUserActivityEvent(event);

            return ResponseEntity.ok(ApiResponse.success("Video unliked", null));

        } catch (Exception e) {
            log.error("Error unliking video", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to unlike video: " + e.getMessage()));
        }
    }

    /**
     * POST /api/videos/{videoId}/share
     * Share video
     */
    @PostMapping("/{videoId}/share")
    public ResponseEntity<ApiResponse<Void>> shareVideo(
            @PathVariable Long videoId,
            @RequestHeader("User-Id") Long userId) {

        try {
            log.info("User {} shared video {}", userId, videoId);

            UserActivityEvent event = UserActivityEvent.builder()
                    .userId(userId)
                    .activityType("SHARE")
                    .targetId(videoId)
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaProducer.sendUserActivityEvent(event);

            return ResponseEntity.ok(ApiResponse.success("Video shared", null));

        } catch (Exception e) {
            log.error("Error sharing video", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to share video: " + e.getMessage()));
        }
    }

    /**
     * POST /api/videos/{videoId}/watchlist
     * Thêm vào watchlist
     */
    @PostMapping("/{videoId}/watchlist")
    public ResponseEntity<ApiResponse<Void>> addToWatchlist(
            @PathVariable Long videoId,
            @RequestHeader("User-Id") Long userId) {

        try {
            log.info("User {} added video {} to watchlist", userId, videoId);

            UserActivityEvent event = UserActivityEvent.builder()
                    .userId(userId)
                    .activityType("ADD_TO_WATCHLIST")
                    .targetId(videoId)
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaProducer.sendUserActivityEvent(event);

            return ResponseEntity.ok(ApiResponse.success("Added to watchlist", null));

        } catch (Exception e) {
            log.error("Error adding to watchlist", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to add to watchlist: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/videos/{videoId}/watchlist
     * Xóa khỏi watchlist
     */
    @DeleteMapping("/{videoId}/watchlist")
    public ResponseEntity<ApiResponse<Void>> removeFromWatchlist(
            @PathVariable Long videoId,
            @RequestHeader("User-Id") Long userId) {

        try {
            log.info("User {} removed video {} from watchlist", userId, videoId);

            UserActivityEvent event = UserActivityEvent.builder()
                    .userId(userId)
                    .activityType("REMOVE_FROM_WATCHLIST")
                    .targetId(videoId)
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaProducer.sendUserActivityEvent(event);

            return ResponseEntity.ok(ApiResponse.success("Removed from watchlist", null));

        } catch (Exception e) {
            log.error("Error removing from watchlist", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to remove from watchlist: " + e.getMessage()));
        }
    }

    /**
     * POST /api/videos/{videoId}/comments
     * Thêm comment
     */
    @PostMapping("/{videoId}/comments")
    public ResponseEntity<ApiResponse<Void>> addComment(
            @PathVariable Long videoId,
            @RequestHeader("User-Id") Long userId,
            @Valid @RequestBody CommentRequest request) {

        try {
            log.info("User {} commented on video {}", userId, videoId);

            String metadata = String.format("{\"content\":\"%s\",\"parentId\":%d}",
                    request.getContent(),
                    request.getParentCommentId() != null ? request.getParentCommentId() : 0);

            UserActivityEvent event = UserActivityEvent.builder()
                    .userId(userId)
                    .activityType("COMMENT")
                    .targetId(videoId)
                    .metadata(metadata)
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaProducer.sendUserActivityEvent(event);

            return ResponseEntity.ok(ApiResponse.success("Comment added", null));

        } catch (Exception e) {
            log.error("Error adding comment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to add comment: " + e.getMessage()));
        }
    }

    /**
     * GET /api/videos/search
     * Tìm kiếm video
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<SearchResponse>> searchVideos(
            @Valid @ModelAttribute VideoSearchRequest request,
            @RequestHeader("User-Id") Long userId) {

        try {
            log.info("User {} searching for: '{}'", userId, request.getQuery());

            SearchResponse response = videoService.search(request);

            // Track search event
            UserActivityEvent event = UserActivityEvent.builder()
                    .userId(userId)
                    .activityType("SEARCH")
                    .metadata("{\"query\":\"" + request.getQuery() + "\"}")
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaProducer.sendUserActivityEvent(event);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error searching videos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Search failed: " + e.getMessage()));
        }
    }

    /**
     * GET /api/videos/filter
     * Filter videos
     */
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<PaginatedResponse<VideoResponse>>> filterVideos(
            @Valid @ModelAttribute VideoFilterRequest request,
            @RequestHeader("User-Id") Long userId) {

        try {
            log.info("Filtering videos - genre: {}, director: {}",
                    request.getGenre(), request.getDirector());

            PaginatedResponse<VideoResponse> response = videoService.filterVideos(request, userId);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error filtering videos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Filter failed: " + e.getMessage()));
        }
    }

    /**
     * GET /api/videos/recommendations
     * Lấy gợi ý phim
     */
    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getRecommendations(
            @RequestHeader("User-Id") Long userId) {

        try {
            log.info("Getting recommendations for user {}", userId);

            RecommendationResponse response = videoService.getRecommendations(userId);

            // Track analytics
            AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                    .eventType("VIEW_RECOMMENDATIONS")
                    .userId(userId)
                    .page("homepage")
                    .action("load_recommendations")
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaProducer.sendAnalyticsEvent(analyticsEvent);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error getting recommendations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get recommendations: " + e.getMessage()));
        }
    }

    /**
     * GET /api/videos/continue-watching
     * Lấy danh sách phim đang xem dở
     */
    @GetMapping("/continue-watching")
    public ResponseEntity<ApiResponse<List<ContinueWatchingResponse>>> getContinueWatching(
            @RequestHeader("User-Id") Long userId) {

        try {
            log.info("Getting continue watching for user {}", userId);

            List<ContinueWatchingResponse> response = videoService.getContinueWatching(userId);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error getting continue watching", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get continue watching: " + e.getMessage()));
        }
    }

    /**
     * GET /api/videos/watchlist
     * Lấy watchlist
     */
    @GetMapping("/watchlist")
    public ResponseEntity<ApiResponse<WatchlistResponse>> getWatchlist(
            @RequestHeader("User-Id") Long userId) {

        try {
            log.info("Getting watchlist for user {}", userId);

            WatchlistResponse response = videoService.getWatchlist(userId);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error getting watchlist", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get watchlist: " + e.getMessage()));
        }
    }

    /**
     * GET /api/videos/history
     * Lấy lịch sử xem
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<WatchHistoryResponse>>> getWatchHistory(
            @RequestHeader("User-Id") Long userId,
            @RequestParam(defaultValue = "20") int limit) {

        try {
            log.info("Getting watch history for user {}", userId);

            List<WatchHistoryResponse> response = videoService.getWatchHistory(userId, limit);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error getting watch history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get watch history: " + e.getMessage()));
        }
    }

    /**
     * GET /api/videos/trending
     * Lấy video trending
     */
    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<TrendingResponse>> getTrending() {

        try {
            log.info("Getting trending videos");

            TrendingResponse response = videoService.getTrending();

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error getting trending videos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get trending videos: " + e.getMessage()));
        }
    }

    /**
     * POST /api/videos/{videoId}/report
     * Report video
     */
    @PostMapping("/{videoId}/report")
    public ResponseEntity<ApiResponse<Void>> reportVideo(
            @PathVariable Long videoId,
            @RequestHeader("User-Id") Long userId,
            @Valid @RequestBody ReportVideoRequest request) {

        try {
            log.warn("User {} reported video {} - reason: {}",
                    userId, videoId, request.getReason());

            String metadata = String.format("{\"reason\":\"%s\",\"description\":\"%s\"}",
                    request.getReason(), request.getDescription());

            UserActivityEvent event = UserActivityEvent.builder()
                    .userId(userId)
                    .activityType("REPORT")
                    .targetId(videoId)
                    .metadata(metadata)
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaProducer.sendUserActivityEvent(event);

            return ResponseEntity.ok(ApiResponse.success("Report submitted", null));

        } catch (Exception e) {
            log.error("Error reporting video", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to report video: " + e.getMessage()));
        }
    }
}