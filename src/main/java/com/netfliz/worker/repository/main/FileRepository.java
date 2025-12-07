package com.netfliz.worker.repository.main;

import com.netfliz.worker.entity.main.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
}
