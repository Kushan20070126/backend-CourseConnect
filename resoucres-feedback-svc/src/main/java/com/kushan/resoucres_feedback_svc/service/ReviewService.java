package com.kushan.resoucres_feedback_svc.service;

import com.kushan.resoucres_feedback_svc.dto.ContentRequests.ReviewRequest;
import com.kushan.resoucres_feedback_svc.model.CourseReview;
import com.kushan.resoucres_feedback_svc.repository.CourseReviewRepository;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewService {
    private final CourseReviewRepository reviews;
    private final MongoTemplate mongo;
    public ReviewService(CourseReviewRepository reviews, MongoTemplate mongo) {
        this.reviews = reviews;
        this.mongo = mongo;
    }

    /** Query 1: retrieve all feedback belonging to one course. */
    public List<CourseReview> feedbackForCourse(String courseId) {
        return reviews.findByCourseIdOrderByCreatedAtDesc(courseId);
    }

    public CourseReview addOrReplace(String courseId, ReviewRequest request, String studentId) {
        if (request == null || request.rating() == null || request.rating() < 1 || request.rating() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }
        if (request.feedback() == null || request.feedback().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Written feedback is required");
        }
        CourseReview review = reviews.findByCourseIdAndStudentId(courseId, studentId).orElseGet(CourseReview::new);
        review.setCourseId(courseId);
        review.setStudentId(studentId);
        review.setStudentName(displayName(studentId));
        review.setRating(request.rating());
        review.setTitle(request.title());
        review.setFeedback(request.feedback().trim());
        review.setAttributes(request.attributes());
        return reviews.save(review);
    }

    /** Query 2: Mongo aggregation ranks courses by average student rating. */
    public List<Map<String, Object>> topRated(int limit) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group("courseId").avg("rating").as("averageRating").count().as("reviewCount"),
                Aggregation.sort(Sort.by(Sort.Order.desc("averageRating"), Sort.Order.desc("reviewCount"))),
                Aggregation.limit(Math.max(1, Math.min(limit, 100)))
        );
        AggregationResults<Document> result = mongo.aggregate(aggregation, "course_reviews", Document.class);
        return result.getMappedResults().stream().map(d -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("courseId", d.getString("_id"));
            item.put("averageRating", d.get("averageRating"));
            item.put("reviewCount", d.get("reviewCount"));
            return item;
        }).toList();
    }

    private String displayName(String email) {
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
