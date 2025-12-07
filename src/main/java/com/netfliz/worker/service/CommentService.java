package com.netfliz.worker.service;

import com.netfliz.worker.entity.analysis.Comment;
import com.netfliz.worker.entity.analysis.CommentLike;
import com.netfliz.worker.entity.analysis.User;
import com.netfliz.worker.model.request.CommentRequest;
import com.netfliz.worker.model.response.CommentResponse;
import com.netfliz.worker.repository.analysis.CommentLikeRepository;
import com.netfliz.worker.repository.analysis.CommentRepository;
import com.netfliz.worker.repository.analysis.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service xử lý Comment functionality
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;

    /**
     * Tạo comment mới
     */
    @Transactional
    public CommentResponse createComment(Long userId, Long videoId, CommentRequest request) {
        try {
            log.info("Creating comment - userId: {}, videoId: {}", userId, videoId);

            // Validate parent comment nếu là reply
            if (request.getParentCommentId() != null) {
                Comment parentComment = commentRepository.findById(request.getParentCommentId())
                        .orElseThrow(() -> new RuntimeException("Parent comment not found"));

                // Không cho phép reply comment đã bị xóa
                if (parentComment.getIsDeleted()) {
                    throw new RuntimeException("Cannot reply to deleted comment");
                }
            }

            Comment comment = Comment.builder()
                    .userId(userId)
                    .videoId(videoId)
                    .content(request.getContent())
                    .parentCommentId(request.getParentCommentId())
                    .likeCount(0)
                    .isEdited(false)
                    .isDeleted(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            comment = commentRepository.save(comment);

            log.info("✓ Comment created - commentId: {}", comment.getId());

            return mapToCommentResponse(comment, userId);

        } catch (Exception e) {
            log.error("Failed to create comment", e);
            throw e;
        }
    }

    /**
     * Lấy comments của video
     */
    public List<CommentResponse> getVideoComments(Long videoId, Long userId, int limit) {
        try {
            log.debug("Getting comments for videoId: {}, limit: {}", videoId, limit);

            Pageable pageable = PageRequest.of(0, limit);

            List<Comment> comments = commentRepository
                    .findByVideoIdAndParentCommentIdIsNullAndIsDeletedFalseOrderByCreatedAtDesc(
                            videoId, pageable);

            return comments.stream()
                    .map(comment -> mapToCommentResponse(comment, userId))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get video comments", e);
            return Collections.emptyList();
        }
    }

    /**
     * Lấy top comments (most liked)
     */
    public List<CommentResponse> getTopComments(Long videoId, Long userId, int limit) {
        try {
            log.debug("Getting top comments for videoId: {}", videoId);

            Pageable pageable = PageRequest.of(0, limit);

            List<Comment> comments = commentRepository
                    .findByVideoIdAndParentCommentIdIsNullAndIsDeletedFalseOrderByLikeCountDescCreatedAtDesc(
                            videoId, pageable);

            return comments.stream()
                    .map(comment -> mapToCommentResponse(comment, userId))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get top comments", e);
            return Collections.emptyList();
        }
    }

    /**
     * Lấy replies của comment
     */
    public List<CommentResponse> getCommentReplies(Long commentId, Long userId) {
        try {
            log.debug("Getting replies for commentId: {}", commentId);

            List<Comment> replies = commentRepository
                    .findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(commentId);

            return replies.stream()
                    .map(reply -> mapToCommentResponse(reply, userId))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get comment replies", e);
            return Collections.emptyList();
        }
    }

    /**
     * Cập nhật comment
     */
    @Transactional
    public CommentResponse updateComment(Long commentId, Long userId, CommentRequest request) {
        try {
            log.info("Updating comment - commentId: {}, userId: {}", commentId, userId);

            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new RuntimeException("Comment not found"));

            // Check ownership
            if (!comment.getUserId().equals(userId)) {
                throw new RuntimeException("User does not own this comment");
            }

            // Check if deleted
            if (comment.getIsDeleted()) {
                throw new RuntimeException("Cannot update deleted comment");
            }

            comment.setContent(request.getContent());
            comment.setIsEdited(true);
            comment.setUpdatedAt(LocalDateTime.now());

            comment = commentRepository.save(comment);

            log.info("✓ Comment updated - commentId: {}", commentId);

            return mapToCommentResponse(comment, userId);

        } catch (Exception e) {
            log.error("Failed to update comment", e);
            throw e;
        }
    }

    /**
     * Xóa comment (soft delete)
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        try {
            log.info("Deleting comment - commentId: {}, userId: {}", commentId, userId);

            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new RuntimeException("Comment not found"));

            // Check ownership
            if (!comment.getUserId().equals(userId)) {
                throw new RuntimeException("User does not own this comment");
            }

            // Soft delete
            comment.setIsDeleted(true);
            comment.setContent("[This comment has been deleted]");
            comment.setUpdatedAt(LocalDateTime.now());

            commentRepository.save(comment);

            log.info("✓ Comment deleted - commentId: {}", commentId);

        } catch (Exception e) {
            log.error("Failed to delete comment", e);
            throw e;
        }
    }

    /**
     * Like comment
     */
    @Transactional
    public void likeComment(Long commentId, Long userId) {
        try {
            log.info("Liking comment - commentId: {}, userId: {}", commentId, userId);

            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new RuntimeException("Comment not found"));

            // Check if already liked
            if (commentLikeRepository.existsByUserIdAndCommentId(userId, commentId)) {
                log.warn("Comment already liked - commentId: {}, userId: {}", commentId, userId);
                return;
            }

            // Create like
            CommentLike like = CommentLike.builder()
                    .userId(userId)
                    .commentId(commentId)
                    .createdAt(LocalDateTime.now())
                    .build();

            commentLikeRepository.save(like);

            // Increment like count
            comment.setLikeCount(comment.getLikeCount() + 1);
            commentRepository.save(comment);

            log.info("✓ Comment liked - commentId: {}, new count: {}",
                    commentId, comment.getLikeCount());

        } catch (Exception e) {
            log.error("Failed to like comment", e);
            throw e;
        }
    }

    /**
     * Unlike comment
     */
    @Transactional
    public void unlikeComment(Long commentId, Long userId) {
        try {
            log.info("Unliking comment - commentId: {}, userId: {}", commentId, userId);

            CommentLike like = commentLikeRepository.findByUserIdAndCommentId(userId, commentId)
                    .orElseThrow(() -> new RuntimeException("Comment not liked"));

            commentLikeRepository.delete(like);

            // Decrement like count
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new RuntimeException("Comment not found"));

            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
            commentRepository.save(comment);

            log.info("✓ Comment unliked - commentId: {}, new count: {}",
                    commentId, comment.getLikeCount());

        } catch (Exception e) {
            log.error("Failed to unlike comment", e);
            throw e;
        }
    }

    /**
     * Map Comment entity to CommentResponse DTO
     */
    private CommentResponse mapToCommentResponse(Comment comment, Long currentUserId) {
        User user = userRepository.findById(comment.getUserId()).orElse(null);

        boolean isLiked = false;
        if (currentUserId != null) {
            isLiked = commentLikeRepository.existsByUserIdAndCommentId(currentUserId, comment.getId());
        }

        // Load replies if not a reply itself
        List<CommentResponse> replies = Collections.emptyList();
        if (comment.getParentCommentId() == null) {
            replies = getCommentReplies(comment.getId(), currentUserId);
        }

        return CommentResponse.builder()
                .id(comment.getId())
                .userId(comment.getUserId())
                .username(user != null ? user.getUsername() : "Unknown")
                .userAvatar("/avatars/" + comment.getUserId() + ".jpg")
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .isLiked(isLiked)
                .isEdited(comment.getIsEdited())
                .isDeleted(comment.getIsDeleted())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replies(replies)
                .replyCount(replies.size())
                .build();
    }

    /**
     * Get user's comment count
     */
    public long getUserCommentCount(Long userId) {
        return commentRepository.countByUserIdAndIsDeletedFalse(userId);
    }
}

// ==========================================
// LIKE SERVICE
// ==========================================

/**
 * Service xử lý Video Like functionality
 */

