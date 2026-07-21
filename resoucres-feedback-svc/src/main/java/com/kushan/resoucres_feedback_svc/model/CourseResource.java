package com.kushan.resoucres_feedback_svc.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deliberately flexible Mongo document. `content` can hold rich-text blocks,
 * file metadata, embeds, links, or future content types without a SQL migration.
 */
@Document("course_resources")
@CompoundIndex(name = "course_kind_created", def = "{'courseId': 1, 'kind': 1, 'createdAt': -1}")
public class CourseResource {
    @Id private String id;
    @Indexed private String courseId;
    private String kind; // LECTURE_NOTE | MULTIMEDIA | SUPPLEMENTARY
    private String title;
    private String description;
    private String url;
    private String mimeType;
    private Map<String, Object> content = new LinkedHashMap<>();
    private String createdBy;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    public String getId() { return id; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Map<String, Object> getContent() { return content; }
    public void setContent(Map<String, Object> content) { this.content = content == null ? new LinkedHashMap<>() : content; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
