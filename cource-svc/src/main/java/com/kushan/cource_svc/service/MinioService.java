package com.kushan.cource_svc.service;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class MinioService {

    @Value("${minio.url}")
    private String minioUrl;
    @Value("${minio.access-key}")
    private String accessKey;
    @Value("${minio.secret-key}")
    private String secretKey;
    @Value("${minio.bucket}")
    private String bucket;

    private MinioClient client;

    @PostConstruct
    public void init() {
        client = MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            // MinIO may be unavailable at startup; uploads will fail with a clear error later.
            System.out.println("[MinIO] bucket init skipped: " + e.getMessage());
        }
    }

    /** Stores a file and returns its object key (e.g. courses/uuid-name.mp4). */
    public String upload(String folder, MultipartFile file) {
        try {
            String key = folder + "/" + UUID.randomUUID() + "-" + sanitize(file.getOriginalFilename());
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to object storage: " + e.getMessage(), e);
        }
    }

    /** Returns a time-limited URL that the browser can use to view/download the object. */
    public String viewUrl(String key) {
        if (key == null || key.isBlank()) return "";
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .method(Method.GET)
                    .expiry(2, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            // Fallback: best-effort direct path (works if the bucket is public).
            return minioUrl + "/" + bucket + "/" + key;
        }
    }

    private String sanitize(String name) {
        if (name == null) return "file";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
