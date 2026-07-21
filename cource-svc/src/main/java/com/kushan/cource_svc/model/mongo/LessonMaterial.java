package com.kushan.cource_svc.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Supplementary material (PDF, slides, datasets, etc.) attached to a lesson.
 * The binary file lives in MinIO; this document holds flexible metadata and a
 * presigned-URL-friendly reference. Multiple materials per lesson are supported.
 */
@Document(collection = "lesson_materials")
public class LessonMaterial {

    @Id
    private String id;

    private Long courseId;
    private Long lessonId;
    private String uploaderId; // lecturer email
    private String title;
    private String fileName;
    private String mimeType;
    private long sizeBytes;
    private String minioKey; // object key under the "materials" folder
    private Instant createdAt = Instant.now();

    public LessonMaterial() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }

    public String getUploaderId() { return uploaderId; }
    public void setUploaderId(String uploaderId) { this.uploaderId = uploaderId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getMinioKey() { return minioKey; }
    public void setMinioKey(String minioKey) { this.minioKey = minioKey; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
