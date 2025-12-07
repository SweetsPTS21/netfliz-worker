package com.netfliz.worker.service;

import com.netfliz.worker.entity.main.FileEntity;
import com.netfliz.worker.model.event.UpdateMovieAssetEvent;
import com.netfliz.worker.repository.main.FileRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FileService {
    private final FileRepository fileRepository;

    public FileEntity saveFile(UpdateMovieAssetEvent.FilePayload payload) {
        var fileEntity = FileEntity.builder()
                .fileName(payload.getFileName())
                .fileType(payload.getFileType())
                .fileSize(payload.getFileSize())
                .fileExtension(payload.getFileExtension())
                .fileDownloadUri(payload.getFileDownloadUri())
                .fileCategory(payload.getFileCategory())
                .fileOwner(payload.getFileOwner())
                .fileUploader(payload.getFileUploader())
                .fileType(payload.getFileType())
                .build();
        return fileRepository.save(fileEntity);
    }
}
