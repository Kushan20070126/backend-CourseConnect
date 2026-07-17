package com.kushan.cource_svc.controller;

import com.kushan.cource_svc.dto.CourseRequest;
import com.kushan.cource_svc.model.Course;
import com.kushan.cource_svc.security.AuthContext;
import com.kushan.cource_svc.service.CourseService;
import com.kushan.cource_svc.service.MinioService;
import io.minio.GetObjectResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/req")
public class CourseController {

    private final CourseService courseService;
    private final AuthContext authContext;
    private final MinioService minioService;

    public CourseController(CourseService courseService, AuthContext authContext, MinioService minioService) {
        this.courseService = courseService;
        this.authContext = authContext;
        this.minioService = minioService;
    }

    @GetMapping("/courses")
    public List<Map<String, Object>> list(@RequestParam(required = false) String category,
                                          @RequestParam(required = false) String level,
                                          @RequestParam(required = false) String q,
                                          @RequestParam(required = false) String sort) {
        return courseService.listPublished(category, level, q, sort);
    }

    @GetMapping("/courses/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        return courseService.getDetail(id, authContext.email().orElse(null));
    }

    // ---- lecturer: create / update / publish / delete ----
    @PostMapping(value = "/courses", consumes = "application/json")
    public ResponseEntity<Map<String, Object>> create(@RequestBody CourseRequest req) {
        requireRole("ROLE_LECTURER");
        String email = authContext.email().orElseThrow();
        Course created = courseService.createCourse(req, email, displayName());
        return ResponseEntity.ok(courseService.getDetail(created.getId(), email));
    }

    @PutMapping(value = "/courses/{id}", consumes = "application/json")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody CourseRequest req) {
        requireRole("ROLE_LECTURER");
        courseService.updateCourse(id, req, authContext.email().orElseThrow());
        return ResponseEntity.ok(Map.of("id", id, "status", "updated"));
    }

    @PostMapping("/courses/{id}/publish")
    public ResponseEntity<Map<String, Object>> publish(@PathVariable Long id) {
        requireRole("ROLE_LECTURER");
        courseService.publish(id, authContext.email().orElseThrow());
        return ResponseEntity.ok(Map.of("id", id, "status", "PUBLISHED"));
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        requireRole("ROLE_LECTURER", "ROLE_ADMIN");
        courseService.delete(id, authContext.email().orElseThrow());
        return ResponseEntity.noContent().build();
    }

    // ---- lecturer: my catalog + monitoring ----
    @GetMapping("/lecturer/courses")
    public List<Map<String, Object>> myCourses() {
        requireRole("ROLE_LECTURER");
        return courseService.lecturerCourses(authContext.email().orElseThrow());
    }

    @GetMapping("/lecturer/stats")
    public Map<String, Object> stats() {
        requireRole("ROLE_LECTURER");
        return courseService.lecturerStats(authContext.email().orElseThrow());
    }

    // ---- uploads (MinIO) ----
    @PostMapping("/courses/{id}/thumbnail")
    public ResponseEntity<Map<String, String>> thumbnail(@PathVariable Long id,
                                                         @RequestParam("file") MultipartFile file) {
        requireRole("ROLE_LECTURER");
        String key = courseService.uploadThumbnail(id, file, authContext.email().orElseThrow());
        return ResponseEntity.ok(Map.of("key", key));
    }

    @PostMapping("/lessons/{lessonId}/video")
    public ResponseEntity<Map<String, String>> lessonVideo(@PathVariable Long lessonId,
                                                           @RequestParam("file") MultipartFile file) {
        requireRole("ROLE_LECTURER");
        String key = courseService.uploadLessonVideo(lessonId, file, authContext.email().orElseThrow());
        return ResponseEntity.ok(Map.of("key", key));
    }

    // ---- media streaming (same-origin proxy target) ----
    // Serves a stored object (thumbnails/videos/materials) by key so the SvelteKit
    // /api/media proxy can forward it. The browser never talks to MinIO directly,
    // which avoids broken images when the dev server is on 127.0.0.1 but MinIO is
    // only reachable via localhost, or when the bucket is private.
    @GetMapping("/media/{*key}")
    public ResponseEntity<InputStreamResource> media(
            @PathVariable String key,
            HttpServletRequest request) {
        String objectKey = key;
        try {
            long fileSize = minioService.getObjectSize(objectKey);
            String contentType = minioService.contentType(objectKey);

            String rangeHeader = request.getHeader(HttpHeaders.RANGE);
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                // Parse range header (e.g., "bytes=0-1023")
                String rangeValue = rangeHeader.substring("bytes=".length());
                String[] ranges = rangeValue.split("-");
                long start = Long.parseLong(ranges[0]);
                long end = ranges.length > 1 && !ranges[1].isEmpty() ? Long.parseLong(ranges[1]) : fileSize - 1;
                if (end >= fileSize) end = fileSize - 1;
                long contentLength = end - start + 1;

                var response = minioService.getObjectRange(objectKey, start, end);
                InputStream inputStream = response;

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(contentType));
                headers.setContentLength(contentLength);
                headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
                headers.set(HttpHeaders.CONTENT_RANGE, String.format("bytes %d-%d/%d", start, end, fileSize));
                headers.setCacheControl("private, max-age=300");

                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .headers(headers)
                        .body(new InputStreamResource(inputStream));
            } else {
                // Full content response
                byte[] bytes = minioService.download(objectKey);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(contentType));
                headers.setContentLength(fileSize);
                headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
                headers.setCacheControl("private, max-age=300");
                return ResponseEntity.ok().headers(headers).body(new InputStreamResource(new java.io.ByteArrayInputStream(bytes)));
            }
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found");
        }
    }

    // -------------------------------------------------- helpers
    private void requireRole(String... roles) {
        for (String r : roles) {
            if (authContext.hasRole(r)) return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
    }

    private String displayName() {
        // Lecturer name isn't in the JWT; use the email locally. The auth-svc
        // email is the stable identifier we store as instructorId/Name.
        return authContext.email().orElse("Lecturer");
    }
}
