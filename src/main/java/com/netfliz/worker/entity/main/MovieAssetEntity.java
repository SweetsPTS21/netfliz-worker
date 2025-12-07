package com.netfliz.worker.entity.main;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.netfliz.worker.entity.main.converter.MovieAssetTypeConverter;
import com.netfliz.worker.entity.main.converter.MovieObjectTypeConverter;
import com.netfliz.worker.entity.main.enums.MovieAssetType;
import com.netfliz.worker.entity.main.enums.MovieObjectType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Date;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "movie_assets")
public class MovieAssetEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "object_id")
    private Long objectId;

    @Column(name = "object_type")
    @Convert(converter = MovieObjectTypeConverter.class)
    private MovieObjectType objectType;

    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "asset_type")
    @Convert(converter = MovieAssetTypeConverter.class)
    private MovieAssetType assetType;

    @Column(name = "name")
    private String name;

    @Column(name = "format")
    private String format;

    @Column(name = "url")
    private String url;

    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode drm;

    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode rendition;

    @Column(name = "created_at", insertable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date createdAt;
}
