package com.kushan.cource_svc.repository.mongo;

import com.kushan.cource_svc.model.mongo.LectureNote;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LectureNoteRepository extends MongoRepository<LectureNote, String> {
    List<LectureNote> findByCourseIdOrderByCreatedAtDesc(Long courseId);
    List<LectureNote> findByLessonIdOrderByCreatedAtDesc(Long lessonId);
}
