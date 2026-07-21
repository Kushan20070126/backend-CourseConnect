package com.kushan.cource_svc.repository.mongo;

import com.kushan.cource_svc.model.mongo.ForumThread;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ForumThreadRepository extends MongoRepository<ForumThread, String> {
    List<ForumThread> findByCourseIdOrderByCreatedAtDesc(Long courseId);
}
