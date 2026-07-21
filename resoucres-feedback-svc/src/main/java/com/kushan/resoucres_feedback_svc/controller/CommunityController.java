package com.kushan.resoucres_feedback_svc.controller;

import com.kushan.resoucres_feedback_svc.dto.ContentRequests.PostRequest;
import com.kushan.resoucres_feedback_svc.dto.ContentRequests.ReviewRequest;
import com.kushan.resoucres_feedback_svc.dto.ContentRequests.ThreadRequest;
import com.kushan.resoucres_feedback_svc.model.CourseReview;
import com.kushan.resoucres_feedback_svc.model.DiscussionThread;
import com.kushan.resoucres_feedback_svc.security.AuthContext;
import com.kushan.resoucres_feedback_svc.service.DiscussionService;
import com.kushan.resoucres_feedback_svc.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community")
public class CommunityController {
    private final ReviewService reviews;
    private final DiscussionService discussions;
    private final AuthContext auth;
    public CommunityController(ReviewService reviews, DiscussionService discussions, AuthContext auth) {
        this.reviews = reviews; this.discussions = discussions; this.auth = auth;
    }

    @GetMapping("/courses/{courseId}/reviews")
    public List<CourseReview> feedback(@PathVariable String courseId) { return reviews.feedbackForCourse(courseId); }

    @PostMapping("/courses/{courseId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseReview review(@PathVariable String courseId, @RequestBody ReviewRequest request) {
        auth.requireRole("STUDENT");
        return reviews.addOrReplace(courseId, request, auth.email());
    }

    @GetMapping("/courses/top-rated")
    public List<Map<String, Object>> topRated(@RequestParam(defaultValue = "10") int limit) { return reviews.topRated(limit); }

    @GetMapping("/courses/{courseId}/threads")
    public List<DiscussionThread> threads(@PathVariable String courseId,
                                          @RequestParam(required = false, name = "q") String keyword) {
        return discussions.search(courseId, keyword);
    }

    @PostMapping("/courses/{courseId}/threads")
    @ResponseStatus(HttpStatus.CREATED)
    public DiscussionThread createThread(@PathVariable String courseId, @RequestBody ThreadRequest request) {
        return discussions.create(courseId, request, auth.email());
    }

    @PostMapping("/threads/{threadId}/posts")
    public DiscussionThread reply(@PathVariable String threadId, @RequestBody PostRequest request) {
        return discussions.post(threadId, request, auth.email());
    }
}
