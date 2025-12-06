package com.netfliz.worker.repository;

import com.netfliz.worker.entity.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    List<SearchLog> findByUserIdOrderByTimestampDesc(Long userId);

    @Query(value = "SELECT query, COUNT(*) as count FROM search_logs " +
            "WHERE timestamp BETWEEN :start AND :end " +
            "GROUP BY query ORDER BY count DESC LIMIT :limit",
            nativeQuery = true)
    List<Map<String, Object>> findTopSearches(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("limit") int limit
    );
}
