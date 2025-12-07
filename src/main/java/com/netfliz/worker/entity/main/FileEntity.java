package com.netfliz.worker.entity.main;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "nf_files")
public class FileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_download_uri", columnDefinition = "TEXT")
    private String fileDownloadUri;

    @Column(name = "file_extension")
    private String fileExtension;

    @Column(name = "file_description", columnDefinition = "TEXT")
    private String fileDescription;

    @Column(name = "file_category")
    private String fileCategory;

    @Column(name = "file_tags")
    private String fileTags;

    @Column(name = "file_status")
    private Integer fileStatus;

    @Column(name = "file_owner")
    private String fileOwner;

    @Column(name = "file_uploader")
    private String fileUploader;

    @Column(name = "created_at", insertable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date updatedAt;
}
