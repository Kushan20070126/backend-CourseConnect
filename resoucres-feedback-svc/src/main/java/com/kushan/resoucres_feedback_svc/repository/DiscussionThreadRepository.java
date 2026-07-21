package com.kushan.resoucres_feedback_svc.repository;

import com.kushan.resoucres_feedback_svc.model.DiscussionThread;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface DiscussionThreadRepository extends MongoRepository<DiscussionThread, String> {
    List<DiscussionThread> findByCourseIdOrderByUpdatedAtDesc(String courseId);
}
