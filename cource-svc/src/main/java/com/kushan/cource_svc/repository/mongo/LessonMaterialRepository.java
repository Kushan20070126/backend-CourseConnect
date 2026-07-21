package com.kushan.cource_svc.repository.mongo;

import com.kushan.cource_svc.model.mongo.LessonMaterial;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LessonMaterialRepository extends MongoRepository<LessonMaterial, String> {
    List<LessonMaterial> findByLessonIdOrderByCreatedAtAsc(Long lessonId);
    List<LessonMaterial> findByCourseIdOrderByCreatedAtAsc(Long courseId);
}
