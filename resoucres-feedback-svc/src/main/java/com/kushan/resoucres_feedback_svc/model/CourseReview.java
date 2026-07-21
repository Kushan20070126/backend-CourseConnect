package com.kushan.resoucres_feedback_svc.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Document("course_reviews")
@CompoundIndex(name = "one_review_per_student_course", def = "{'courseId': 1, 'studentId': 1}", unique = true)
@CompoundIndex(name = "course_newest", def = "{'courseId': 1, 'createdAt': -1}")
public class CourseReview {
    @Id private String id;
    @Indexed private String courseId;
    private String studentId;
    private String studentName;
    private int rating;
    private String title;
    private String feedback;
    // Optional survey answers, sentiment, browser context, or future fields.
    private Map<String, Object> attributes = new LinkedHashMap<>();
    @CreatedDate private Instant createdAt;

    public String getId() { return id; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes == null ? new LinkedHashMap<>() : attributes; }
    public Instant getCreatedAt() { return createdAt; }
}
