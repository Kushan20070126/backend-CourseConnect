package com.kushan.cource_svc.controller;

import com.kushan.cource_svc.security.AuthContext;
import com.kushan.cource_svc.service.EnrollmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/req")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final AuthContext authContext;

    public EnrollmentController(EnrollmentService enrollmentService, AuthContext authContext) {
        this.enrollmentService = enrollmentService;
        this.authContext = authContext;
    }

    @PostMapping("/courses/{id}/enroll")
    public ResponseEntity<Map<String, Object>> enroll(@PathVariable Long id) {
        requireStudent();
        return ResponseEntity.ok(enrollmentService.enroll(id, authContext.email().orElseThrow(), displayName()));
    }

    @GetMapping("/my-courses")
    public List<Map<String, Object>> myCourses() {
        requireStudent();
        return enrollmentService.myCourses(authContext.email().orElseThrow());
    }

    @GetMapping("/courses/{id}/learn")
    public Map<String, Object> learn(@PathVariable Long id) {
        requireStudent();
        return enrollmentService.learn(id, authContext.email().orElseThrow());
    }

    @PostMapping("/courses/{id}/lessons/{lessonId}/complete")
    public Map<String, Object> complete(@PathVariable Long id, @PathVariable Long lessonId) {
        requireStudent();
        return enrollmentService.completeLesson(id, lessonId, authContext.email().orElseThrow());
    }

    private void requireStudent() {
        if (!authContext.hasRole("ROLE_STUDENT")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Students only");
        }
    }

    private String displayName() {
        return authContext.email().orElse("Student");
    }
}
