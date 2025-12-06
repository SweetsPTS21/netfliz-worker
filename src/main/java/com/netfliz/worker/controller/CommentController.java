package com.netfliz.worker.controller;

import com.netfliz.worker.model.request.CommentRequest;
import com.netfliz.worker.model.response.ApiResponse;
import com.netfliz.worker.model.response.CommentResponse;
import com.netfliz.worker.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Comment operations
 */
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentService commentService;

    /**
     * GET /api/comments/video/{videoId}
     * Lấy comments của video (sắp xếp theo thời gian mới nhất)
     */
    @GetMapping("/video/{videoId}")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getVideoComments(
            @PathVariable Long videoId,
            @RequestHeader(value = "User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "20") int limit) {

        try {
            log.info("Getting comments for videoId: {}, limit: {}", videoId, limit);

            List<CommentResponse> comments = commentService.getVideoComments(videoId, userId, limit);

            return ResponseEntity.ok(ApiResponse.success(comments));

        } catch (Exception e) {
            log.error("Error getting video comments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get comments: " + e.getMessage()));
        }
    }

    /**
     * GET /api/comments/video/{videoId}/top
     * Lấy top comments (sắp xếp theo likes)
     */
    @GetMapping("/video/{videoId}/top")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getTopComments(
            @PathVariable Long videoId,
            @RequestHeader(value = "User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "10") int limit) {

        try {
            log.info("Getting top comments for videoId: {}, limit: {}", videoId, limit);

            List<CommentResponse> comments = commentService.getTopComments(videoId, userId, limit);

            return ResponseEntity.ok(ApiResponse.success(comments));

        } catch (Exception e) {
            log.error("Error getting top comments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get top comments: " + e.getMessage()));
        }
    }

    /**
     * GET /api/comments/{commentId}/replies
     * Lấy replies của comment
     */
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getCommentReplies(
            @PathVariable Long commentId,
            @RequestHeader(value = "User-Id", required = false) Long userId) {

        try {
            log.info("Getting replies for commentId: {}", commentId);

            List<CommentResponse> replies = commentService.getCommentReplies(commentId, userId);

            return ResponseEntity.ok(ApiResponse.success(replies));

        } catch (Exception e) {
            log.error("Error getting comment replies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get replies: " + e.getMessage()));
        }
    }

    /**
     * POST /api/comments/video/{videoId}
     * Tạo comment mới cho video
     */
    @PostMapping("/video/{videoId}")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable Long videoId,
            @RequestHeader("User-Id") Long userId,
            @Valid @RequestBody CommentRequest request) {

        try {
            log.info("Creating comment - userId: {}, videoId: {}", userId, videoId);

            CommentResponse comment = commentService.createComment(userId, videoId, request);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Comment created", comment));

        } catch (Exception e) {
            log.error("Error creating comment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create comment: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/comments/{commentId}
     * Cập nhật comment
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long commentId,
            @RequestHeader("User-Id") Long userId,
            @Valid @RequestBody CommentRequest request) {

        try {
            log.info("Updating comment - commentId: {}, userId: {}", commentId, userId);

            CommentResponse comment = commentService.updateComment(commentId, userId, request);

            return ResponseEntity.ok(ApiResponse.success("Comment updated", comment));

        } catch (Exception e) {
            log.error("Error updating comment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update comment: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/comments/{commentId}
     * Xóa comment (soft delete)
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @RequestHeader("User-Id") Long userId) {

        try {
            log.info("Deleting comment - commentId: {}, userId: {}", commentId, userId);

            commentService.deleteComment(commentId, userId);

            return ResponseEntity.ok(ApiResponse.success("Comment deleted", null));

        } catch (Exception e) {
            log.error("Error deleting comment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete comment: " + e.getMessage()));
        }
    }

    /**
     * POST /api/comments/{commentId}/like
     * Like comment
     */
    @PostMapping("/{commentId}/like")
    public ResponseEntity<ApiResponse<Void>> likeComment(
            @PathVariable Long commentId,
            @RequestHeader("User-Id") Long userId) {

        try {
            log.info("Liking comment - commentId: {}, userId: {}", commentId, userId);

            commentService.likeComment(commentId, userId);

            return ResponseEntity.ok(ApiResponse.success("Comment liked", null));

        } catch (Exception e) {
            log.error("Error liking comment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to like comment: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/comments/{commentId}/like
     * Unlike comment
     */
    @DeleteMapping("/{commentId}/like")
    public ResponseEntity<ApiResponse<Void>> unlikeComment(
            @PathVariable Long commentId,
            @RequestHeader("User-Id") Long userId) {

        try {
            log.info("Unliking comment - commentId: {}, userId: {}", commentId, userId);

            commentService.unlikeComment(commentId, userId);

            return ResponseEntity.ok(ApiResponse.success("Comment unliked", null));

        } catch (Exception e) {
            log.error("Error unliking comment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to unlike comment: " + e.getMessage()));
        }
    }
}
