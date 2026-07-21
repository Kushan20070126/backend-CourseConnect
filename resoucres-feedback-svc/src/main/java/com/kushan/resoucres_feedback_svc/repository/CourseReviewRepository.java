package com.kushan.resoucres_feedback_svc.repository;

import com.kushan.resoucres_feedback_svc.model.CourseReview;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface CourseReviewRepository extends MongoRepository<CourseReview, String> {
    List<CourseReview> findByCourseIdOrderByCreatedAtDesc(String courseId);
    Optional<CourseReview> findByCourseIdAndStudentId(String courseId, String studentId);
}
