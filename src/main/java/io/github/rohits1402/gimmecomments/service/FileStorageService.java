package io.github.rohits1402.gimmecomments.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String store(MultipartFile file);

    void delete(String fileUrl);
}
