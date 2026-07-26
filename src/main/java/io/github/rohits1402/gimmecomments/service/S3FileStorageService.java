package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

@Service
@Profile("prod")
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3;
    private final String bucket;
    private final String baseUrl;

    public S3FileStorageService(S3Client s3,
                                @Value("${app.s3.bucket}") String bucket,
                                @Value("${app.s3.region}") String region) {
        this.s3 = s3;
        this.bucket = bucket;
        this.baseUrl = "https://" + bucket + ".s3." + region + ".amazonaws.com";
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
        String key = "profile-images/" + UUID.randomUUID() + extension;

        try {

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();
            s3.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return baseUrl + "/" + key;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to upload file to S3", e);
        }

    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank() || !fileUrl.startsWith(baseUrl)) {
            return;
        }
        String key = fileUrl.substring(baseUrl.length() + 1);
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
}
