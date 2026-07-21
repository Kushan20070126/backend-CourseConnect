package com.kushan.cource_svc.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Free-form lecture notes attached to a course. Stored in MongoDB because the
 * content is unstructured and evolves independently of the relational course row.
 */
@Document(collection = "lecture_notes")
public class LectureNote {

    @Id
    private String id;

    private Long courseId;
    private Long lessonId; // optional: null = course-level note
    private String authorId; // instructor/lecturer email
    private String authorName;
    private String title;
    private String body; // markdown / plain text
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public LectureNote() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
