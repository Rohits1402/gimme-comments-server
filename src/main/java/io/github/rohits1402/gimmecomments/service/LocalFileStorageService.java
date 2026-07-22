package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Profile("dev")
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final Path uploadDir;
    private final String baseUrl;

    public LocalFileStorageService(@Value("${app.upload.dir}") String dir, @Value("${app.upload.base-url}") String baseUrl) {
        this.uploadDir = Paths.get(dir).toAbsolutePath().normalize();
        this.baseUrl = baseUrl;
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory", e);
        }
    }

    @Override
    public String store(MultipartFile file) {

        if (file == null || file.isEmpty())
            throw new BadRequestException("No file provided");

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed");
        }

        String extension = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains("."))
            extension = original.substring(original.lastIndexOf("."));
        String filename = UUID.randomUUID() + extension;
        try {

            Path target = uploadDir.resolve(filename);
            file.transferTo(target);
            return baseUrl + "/" + filename;

        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank() || !fileUrl.startsWith(baseUrl))
            return;

        String filename = fileUrl.substring(fileUrl.lastIndexOf('/' + 1));
        try {
            Files.deleteIfExists(uploadDir.resolve(filename));
        } catch (IOException e) {
            log.warn("Could not delete file {}", fileUrl, e);
        }
    }
}
