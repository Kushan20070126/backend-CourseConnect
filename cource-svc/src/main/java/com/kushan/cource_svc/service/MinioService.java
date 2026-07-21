package com.kushan.cource_svc.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
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
            if (file == null || file.isEmpty()) {
                throw new RuntimeException("Upload rejected: received an empty file (0 bytes).");
            }
            String key = folder + "/" + UUID.randomUUID() + "-" + sanitize(file.getOriginalFilename());
            // Read the full payload into memory first. Relying on
            // `file.getInputStream()` + `file.getSize()` with the `-1` (unknown)
            // part-size is fragile: Spring may have already consumed the stream
            // while writing it to the multipart temp location (file-size-threshold
            // in application.properties), which can yield a truncated/empty object
            // in MinIO. Using the exact bytes we read makes the stored size match
            // the uploaded file reliably.
            byte[] data = file.getBytes();
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(new ByteArrayInputStream(data), data.length, -1)
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

    /** Streams an object's bytes back (used by the backend media GET endpoint). */
    public byte[] download(String key) {
        try (java.io.InputStream in = client.getObject(
                io.minio.GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            return in.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read object from storage: " + e.getMessage(), e);
        }
    }

    /** Streams an object with Range support for video streaming. */
    public GetObjectResponse getObjectRange(String key, long start, long end) {
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .offset(start)
                    .length(end - start + 1)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read object range from storage: " + e.getMessage(), e);
        }
    }

    /** Returns the stored object's size in bytes. */
    public long getObjectSize(String key) {
        try {
            StatObjectResponse stat = client.statObject(io.minio.StatObjectArgs.builder().bucket(bucket).object(key).build());
            return stat.size();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get object size: " + e.getMessage(), e);
        }
    }

    /** Returns the stored object's content type, or a default if unknown. */
    public String contentType(String key) {
        try {
            return client.statObject(io.minio.StatObjectArgs.builder().bucket(bucket).object(key).build())
                    .contentType();
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }

    private String sanitize(String name) {
        if (name == null) return "file";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
