package com.netfliz.worker.repository;

import com.netfliz.worker.entity.Comment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Get top-level comments (not replies)
    List<Comment> findByVideoIdAndParentCommentIdIsNullAndIsDeletedFalseOrderByCreatedAtDesc(
            Long videoId, Pageable pageable
    );

    // Get top comments by likes
    List<Comment> findByVideoIdAndParentCommentIdIsNullAndIsDeletedFalseOrderByLikeCountDescCreatedAtDesc(
            Long videoId, Pageable pageable
    );

    // Get replies to a comment
    List<Comment> findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(Long parentCommentId);

    // Count comments for video (excluding deleted)
    long countByVideoIdAndIsDeletedFalse(Long videoId);

    // Count user's comments (excluding deleted)
    long countByUserIdAndIsDeletedFalse(Long userId);

    // Get all comments by user
    List<Comment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Collection<Comment> findByVideoIdOrderByLikeCountDescCreatedAtDesc(Long videoId, PageRequest of);
}