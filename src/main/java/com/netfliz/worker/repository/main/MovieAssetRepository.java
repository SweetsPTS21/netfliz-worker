package com.netfliz.worker.repository.main;

import com.netfliz.worker.entity.main.MovieAssetEntity;
import com.netfliz.worker.entity.main.enums.MovieObjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface MovieAssetRepository extends JpaRepository<MovieAssetEntity, Long> {

    @Query("SELECT a FROM MovieAssetEntity a WHERE a.objectId = :movieId AND a.objectType = :objectType")
    List<MovieAssetEntity> findByObjectId(Long movieId, MovieObjectType objectType);

    @Query("SELECT a FROM MovieAssetEntity a WHERE a.objectId IN :objectIds AND a.objectType = :objectType")
    List<MovieAssetEntity> findByObjectIds(Collection<Long> objectIds, MovieObjectType objectType);

    @Modifying
    @Query("DELETE FROM MovieAssetEntity a WHERE a.objectId = :episodeId AND a.objectType = :objectType")
    void deleteAllByObjectId(Long episodeId, MovieObjectType objectType);
}
