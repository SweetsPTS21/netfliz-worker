package com.netfliz.worker.model.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.netfliz.worker.constant.KafkaEventType;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "eventType")
@JsonTypeName(KafkaEventType.UPDATE_MOVIE_ASSET)
public class UpdateMovieAssetEvent extends BaseEvent<UpdateMovieAssetEvent.MovieAssetPayload> {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeName("movieAssetPayload")
    public static class MovieAssetPayload {
        private Long objectId;
        private Integer objectType;
        private Integer assetType; // e.g., "TRAILER", "MOVIE", "TEASER"
        private String name;
        private String format;
        private String url;
        private String drm;
        private String rendition;
        private FilePayload file;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilePayload {
        private String fileCategory;
        private String fileDownloadUri;
        private String fileExtension;
        private String fileName;
        private String fileOwner;
        private Long fileSize;
        private String fileType;
        private String fileUploader;
    }

    @Override
    public String getTopic() {
        return "update-movie-asset";
    }

    // Factory method to create event with payload
    public static UpdateMovieAssetEvent create(MovieAssetPayload payload) {
        UpdateMovieAssetEvent event = new UpdateMovieAssetEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(KafkaEventType.UPDATE_MOVIE_ASSET);
        event.setTimestamp(LocalDateTime.now());
        event.setSource("encoder-service");
        event.setPayload(payload);

        return event;
    }
}
