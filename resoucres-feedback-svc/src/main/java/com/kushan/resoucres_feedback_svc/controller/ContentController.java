package com.kushan.resoucres_feedback_svc.controller;

import com.kushan.resoucres_feedback_svc.dto.ContentRequests.ResourceRequest;
import com.kushan.resoucres_feedback_svc.model.CourseResource;
import com.kushan.resoucres_feedback_svc.security.AuthContext;
import com.kushan.resoucres_feedback_svc.service.ContentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/content/courses/{courseId}/resources")
public class ContentController {
    private final ContentService content;
    private final AuthContext auth;
    public ContentController(ContentService content, AuthContext auth) { this.content = content; this.auth = auth; }

    @GetMapping
    public List<CourseResource> list(@PathVariable String courseId, @RequestParam(required = false) String kind) {
        return content.list(courseId, kind);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResource create(@PathVariable String courseId, @RequestBody ResourceRequest request) {
        auth.requireRole("LECTURER", "ADMIN");
        return content.create(courseId, request, auth.email());
    }
}
