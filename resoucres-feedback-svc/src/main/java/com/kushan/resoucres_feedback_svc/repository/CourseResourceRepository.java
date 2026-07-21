package com.kushan.resoucres_feedback_svc.repository;

import com.kushan.resoucres_feedback_svc.model.CourseResource;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface CourseResourceRepository extends MongoRepository<CourseResource, String> {
    List<CourseResource> findByCourseIdOrderByCreatedAtDesc(String courseId);
    List<CourseResource> findByCourseIdAndKindOrderByCreatedAtDesc(String courseId, String kind);
}
