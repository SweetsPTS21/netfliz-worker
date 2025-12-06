package com.netfliz.worker.repository;

import com.netfliz.worker.entity.ViewHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ViewHistoryRepository extends JpaRepository<ViewHistory, Long> {

    List<ViewHistory> findByUserIdOrderByViewStartTimeDesc(Long userId);

    List<ViewHistory> findByUserIdOrderByViewStartTimeDesc(Long userId, Pageable pageable);

    Optional<ViewHistory> findByUserIdAndVideoId(Long userId, Long videoId);

    @Query("SELECT COUNT(DISTINCT vh.userId) FROM ViewHistory vh WHERE vh.videoId = :videoId")
    long countDistinctUsersByVideoId(@Param("videoId") Long videoId);

    @Query("SELECT vh.videoId, COUNT(vh) as count FROM ViewHistory vh " +
            "WHERE vh.userId = :userId GROUP BY vh.videoId ORDER BY count DESC")
    List<Object[]> findMostWatchedByUser(@Param("userId") Long userId, Pageable pageable);
}
