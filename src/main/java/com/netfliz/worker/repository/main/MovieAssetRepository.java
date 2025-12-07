package com.netfliz.worker.repository.main;

import com.netfliz.worker.entity.main.MovieAssetEntity;
import com.netfliz.worker.entity.main.enums.MovieAssetType;
import com.netfliz.worker.entity.main.enums.MovieObjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface MovieAssetRepository extends JpaRepository<MovieAssetEntity, Long> {

    @Query("SELECT a FROM MovieAssetEntity a WHERE a.objectId = :objectId AND a.objectType = :objectType AND a.assetType = :assetType")
    MovieAssetEntity findByObjectId(Long objectId, MovieObjectType objectType, MovieAssetType assetType);

    @Query("SELECT a FROM MovieAssetEntity a WHERE a.objectId IN :objectIds AND a.objectType = :objectType")
    List<MovieAssetEntity> findByObjectIds(Collection<Long> objectIds, MovieObjectType objectType);

    @Modifying
    @Query("DELETE FROM MovieAssetEntity a WHERE a.objectId = :episodeId AND a.objectType = :objectType")
    void deleteAllByObjectId(Long episodeId, MovieObjectType objectType);
}
