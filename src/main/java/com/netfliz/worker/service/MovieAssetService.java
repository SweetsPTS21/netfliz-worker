package com.netfliz.worker.service;

import com.netfliz.worker.entity.main.MovieAssetEntity;
import com.netfliz.worker.entity.main.enums.MovieAssetType;
import com.netfliz.worker.entity.main.enums.MovieObjectType;
import com.netfliz.worker.model.event.UpdateMovieAssetEvent;
import com.netfliz.worker.repository.main.MovieAssetRepository;
import com.netfliz.worker.utils.JsonUtils;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@AllArgsConstructor
public class MovieAssetService {
    private final MovieAssetRepository movieAssetRepository;

    public void saveMovieAsset(UpdateMovieAssetEvent.MovieAssetPayload payload, Long fileId) {
        var entity = movieAssetRepository.findByObjectId(payload.getObjectId(),
                MovieObjectType.fromId(payload.getObjectType()),
                MovieAssetType.fromId(payload.getAssetType()));
        if (Objects.isNull(entity)) {
            createMovieAsset(payload, fileId);
        }

        entity.setName(payload.getName());
        entity.setFileId(fileId);
        entity.setUrl(payload.getUrl());
        entity.setFormat(payload.getFormat());
        entity.setDrm(JsonUtils.parse(payload.getDrm()));
        entity.setRendition(JsonUtils.parse(payload.getRendition()));

        movieAssetRepository.save(entity);
    }

    private void createMovieAsset(UpdateMovieAssetEvent.MovieAssetPayload payload, Long fileId) {
        movieAssetRepository.save(MovieAssetEntity.builder()
                .objectId(payload.getObjectId())
                .objectType(MovieObjectType.fromId(payload.getObjectType()))
                .assetType(MovieAssetType.fromId(payload.getAssetType()))
                .format(payload.getFormat())
                .drm(JsonUtils.parse(payload.getDrm()))
                .rendition(JsonUtils.parse(payload.getRendition()))
                .fileId(fileId)
                .url(payload.getUrl())
                .build());
    }
}
