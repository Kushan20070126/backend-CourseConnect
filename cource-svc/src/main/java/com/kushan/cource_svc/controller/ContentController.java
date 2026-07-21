package com.kushan.cource_svc.controller;

import com.kushan.cource_svc.security.AuthContext;
import com.kushan.cource_svc.service.MongoContentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/req")
public class ContentController {

    private final MongoContentService contentService;
    private final AuthContext authContext;

    public ContentController(MongoContentService contentService, AuthContext authContext) {
        this.contentService = contentService;
        this.authContext = authContext;
    }

    @PostMapping("/lessons/{lessonId}/materials")
    public ResponseEntity<Map<String, Object>> uploadMaterial(
            @PathVariable Long lessonId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) {
        requireRole("ROLE_LECTURER");

        Long courseId = resolveCourseId(lessonId);
        var m = contentService.addMaterial(courseId, lessonId, authContext.email().orElseThrow(), title, file);
        return ResponseEntity.ok(Map.of(
                "id", m.getId(), "title", m.getTitle(), "fileName", m.getFileName(),
                "mimeType", m.getMimeType() == null ? "" : m.getMimeType(), "sizeBytes", m.getSizeBytes()
        ));
    }

    @GetMapping("/lessons/{lessonId}/materials")
    public List<Map<String, Object>> listMaterials(@PathVariable Long lessonId) {
        return contentService.materialsForLesson(lessonId);
    }

    @PostMapping(value = "/courses/{id}/notes", consumes = "application/json")
    public ResponseEntity<Map<String, Object>> addNote(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        requireRole("ROLE_LECTURER");
        Long lessonId = body.get("lessonId") != null ? Long.valueOf(body.get("lessonId").toString()) : null;
        var n = contentService.addNote(id, lessonId, authContext.email().orElseThrow(),
                displayName(), (String) body.get("title"), (String) body.get("body"));
        return ResponseEntity.ok(Map.of("id", n.getId()));
    }

    @GetMapping("/courses/{id}/notes")
    public List<Map<String, Object>> listNotes(@PathVariable Long id) {
        return contentService.notesForCourse(id);
    }

    @PostMapping(value = "/courses/{id}/reviews", consumes = "application/json")
    public ResponseEntity<Map<String, Object>> addReview(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        requireRole("ROLE_STUDENT");
        int rating = body.get("rating") != null ? Integer.parseInt(body.get("rating").toString()) : 5;
        var r = contentService.addReview(id, authContext.email().orElseThrow(), displayName(),
                rating, (String) body.get("title"), (String) body.get("body"));
        return ResponseEntity.ok(Map.of("id", r.getId()));
    }

    @GetMapping("/courses/{id}/reviews")
    public Map<String, Object> listReviews(@PathVariable Long id) {
        return Map.of("summary", contentService.reviewSummary(id), "items", contentService.reviewsForCourse(id));
    }

    // -------------------------------------------------- forum / Q&A
    @PostMapping(value = "/courses/{id}/forum", consumes = "application/json")
    public ResponseEntity<Map<String, Object>> addThread(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        var t = contentService.addThread(id, authContext.email().orElseThrow(), displayName(),
                (String) body.get("title"), (String) body.get("body"));
        return ResponseEntity.ok(Map.of("id", t.getId()));
    }

    @PostMapping(value = "/forum/{threadId}/posts", consumes = "application/json")
    public ResponseEntity<Map<String, Object>> addPost(
            @PathVariable String threadId,
            @RequestBody Map<String, Object> body) {
        boolean isInstructor = authContext.hasRole("ROLE_LECTURER") || authContext.hasRole("ROLE_ADMIN");
        var t = contentService.addPost(threadId, authContext.email().orElseThrow(), displayName(), isInstructor,
                (String) body.get("body"));
        return ResponseEntity.ok(Map.of("id", t.getId(), "posts", t.getPosts().size()));
    }

    @GetMapping("/courses/{id}/forum")
    public List<Map<String, Object>> listForum(@PathVariable Long id) {
        return contentService.forumForCourse(id);
    }

    private Long resolveCourseId(Long lessonId) {

        return lessonId;
    }

    private void requireRole(String... roles) {
        for (String r : roles) {
            if (authContext.hasRole(r)) return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
    }

    private String displayName() {
        return authContext.email().orElse("User");
    }
}
