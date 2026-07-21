package com.kushan.cource_svc.repository.mongo;

import com.kushan.cource_svc.model.mongo.Review;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByCourseIdOrderByCreatedAtDesc(Long courseId);
    long countByCourseId(Long courseId);
}
