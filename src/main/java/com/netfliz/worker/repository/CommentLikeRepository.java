package com.netfliz.worker.repository;

import com.netfliz.worker.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByUserIdAndCommentId(Long userId, Long commentId);

    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    void deleteByUserIdAndCommentId(Long userId, Long commentId);

    long countByCommentId(Long commentId);

    List<CommentLike> findByUserId(Long userId);
}
