package com.kushan.resoucres_feedback_svc.service;

import com.kushan.resoucres_feedback_svc.dto.ContentRequests.ResourceRequest;
import com.kushan.resoucres_feedback_svc.model.CourseResource;
import com.kushan.resoucres_feedback_svc.repository.CourseResourceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
public class ContentService {
    private static final Set<String> KINDS = Set.of("LECTURE_NOTE", "MULTIMEDIA", "SUPPLEMENTARY");
    private final CourseResourceRepository resources;
    public ContentService(CourseResourceRepository resources) { this.resources = resources; }

    public List<CourseResource> list(String courseId, String kind) {
        return kind == null || kind.isBlank()
                ? resources.findByCourseIdOrderByCreatedAtDesc(courseId)
                : resources.findByCourseIdAndKindOrderByCreatedAtDesc(courseId, normalizeKind(kind));
    }

    public CourseResource create(String courseId, ResourceRequest request, String author) {
        if (request == null || blank(request.title())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A resource title is required");
        }
        CourseResource resource = new CourseResource();
        resource.setCourseId(courseId);
        resource.setKind(normalizeKind(request.kind()));
        resource.setTitle(request.title().trim());
        resource.setDescription(request.description());
        resource.setUrl(request.url());
        resource.setMimeType(request.mimeType());
        resource.setContent(request.content());
        resource.setCreatedBy(author);
        return resources.save(resource);
    }

    private String normalizeKind(String kind) {
        String result = kind == null ? "SUPPLEMENTARY" : kind.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        if (!KINDS.contains(result)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported resource kind");
        return result;
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
