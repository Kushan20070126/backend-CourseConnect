package com.kushan.cource_svc.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Student review + rating for a course. Stored in MongoDB so open-ended
 * feedback and rich structured ratings live outside the relational schema.
 */
@Document(collection = "reviews")
public class Review {

    @Id
    private String id;

    private Long courseId;
    private String studentId; // email
    private String studentName;
    private int rating; // 1..5
    private String title;
    private String body; // open-ended feedback (may be empty)
    private Instant createdAt = Instant.now();

    public Review() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
