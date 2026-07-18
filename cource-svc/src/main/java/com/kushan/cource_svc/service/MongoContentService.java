package com.kushan.cource_svc.service;

import com.kushan.cource_svc.model.mongo.ForumPost;
import com.kushan.cource_svc.model.mongo.ForumThread;
import com.kushan.cource_svc.model.mongo.LectureNote;
import com.kushan.cource_svc.model.mongo.LessonMaterial;
import com.kushan.cource_svc.model.mongo.Review;
import com.kushan.cource_svc.repository.mongo.ForumThreadRepository;
import com.kushan.cource_svc.repository.mongo.LectureNoteRepository;
import com.kushan.cource_svc.repository.mongo.LessonMaterialRepository;
import com.kushan.cource_svc.repository.mongo.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Complementary NoSQL (MongoDB) content service: lecture notes, lesson
 * materials (binaries in MinIO), student reviews/ratings, and the discussion
 * forum. Unstructured/variable-shape data lives here, not in the Oracle schema.
 */
@Service
public class MongoContentService {

    private final LessonMaterialRepository materialRepository;
    private final LectureNoteRepository noteRepository;
    private final ReviewRepository reviewRepository;
    private final ForumThreadRepository forumRepository;
    private final MinioService minioService;

    public MongoContentService(LessonMaterialRepository materialRepository,
                               LectureNoteRepository noteRepository,
                               ReviewRepository reviewRepository,
                               ForumThreadRepository forumRepository,
                               MinioService minioService) {
        this.materialRepository = materialRepository;
        this.noteRepository = noteRepository;
        this.reviewRepository = reviewRepository;
        this.forumRepository = forumRepository;
        this.minioService = minioService;
    }

    // ---------------------------------------------------------------- materials
    public LessonMaterial addMaterial(Long courseId, Long lessonId, String uploaderId,
                                      String title, MultipartFile file) {
        String key = minioService.upload("materials", file);
        LessonMaterial m = new LessonMaterial();
        m.setCourseId(courseId);
        m.setLessonId(lessonId);
        m.setUploaderId(uploaderId);
        m.setTitle(title != null && !title.isBlank() ? title : file.getOriginalFilename());
        m.setFileName(file.getOriginalFilename());
        m.setMimeType(file.getContentType());
        m.setSizeBytes(file.getSize());
        m.setMinioKey(key);
        m.setCreatedAt(Instant.now());
        return materialRepository.save(m);
    }

    /** Returns materials with a fresh presigned URL for each. */
    public List<Map<String, Object>> materialsForLesson(Long lessonId) {
        return materialRepository.findByLessonIdOrderByCreatedAtAsc(lessonId).stream()
                .map(this::toMaterialView)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toMaterialView(LessonMaterial m) {
        return Map.of(
                "id", m.getId(),
                "title", m.getTitle(),
                "fileName", m.getFileName() == null ? "" : m.getFileName(),
                "mimeType", m.getMimeType() == null ? "" : m.getMimeType(),
                "sizeBytes", m.getSizeBytes(),
                "url", minioService.viewUrl(m.getMinioKey())
        );
    }

    // ---------------------------------------------------------------- notes
    public LectureNote addNote(Long courseId, Long lessonId, String authorId,
                               String authorName, String title, String body) {
        LectureNote n = new LectureNote();
        n.setCourseId(courseId);
        n.setLessonId(lessonId);
        n.setAuthorId(authorId);
        n.setAuthorName(authorName);
        n.setTitle(title);
        n.setBody(body);
        n.setCreatedAt(Instant.now());
        n.setUpdatedAt(Instant.now());
        return noteRepository.save(n);
    }

    public List<Map<String, Object>> notesForLesson(Long lessonId) {
        return noteRepository.findByLessonIdOrderByCreatedAtDesc(lessonId).stream()
                .map(this::toNoteView)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> notesForCourse(Long courseId) {
        return noteRepository.findByCourseIdOrderByCreatedAtDesc(courseId).stream()
                .map(this::toNoteView)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toNoteView(LectureNote n) {
        return Map.of(
                "id", n.getId(),
                "courseId", n.getCourseId(),
                "lessonId", n.getLessonId() == null ? "" : n.getLessonId(),
                "authorName", n.getAuthorName() == null ? "" : n.getAuthorName(),
                "title", n.getTitle() == null ? "" : n.getTitle(),
                "body", n.getBody() == null ? "" : n.getBody(),
                "createdAt", n.getCreatedAt() == null ? "" : n.getCreatedAt().toString()
        );
    }

    // ---------------------------------------------------------------- reviews
    public Review addReview(Long courseId, String studentId, String studentName,
                            int rating, String title, String body) {
        Review r = new Review();
        r.setCourseId(courseId);
        r.setStudentId(studentId);
        r.setStudentName(studentName);
        r.setRating(Math.max(1, Math.min(5, rating)));
        r.setTitle(title);
        r.setBody(body);
        r.setCreatedAt(Instant.now());
        return reviewRepository.save(r);
    }

    public List<Map<String, Object>> reviewsForCourse(Long courseId) {
        return reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId).stream()
                .map(this::toReviewView)
                .collect(Collectors.toList());
    }

    public Map<String, Object> reviewSummary(Long courseId) {
        List<Review> all = reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
        double avg = all.stream().mapToInt(Review::getRating).average().orElse(0.0);
        return Map.of(
                "count", all.size(),
                "average", Math.round(avg * 10.0) / 10.0
        );
    }

    private Map<String, Object> toReviewView(Review r) {
        return Map.of(
                "id", r.getId(),
                "studentName", r.getStudentName() == null ? "Anonymous" : r.getStudentName(),
                "rating", r.getRating(),
                "title", r.getTitle() == null ? "" : r.getTitle(),
                "body", r.getBody() == null ? "" : r.getBody(),
                "createdAt", r.getCreatedAt() == null ? "" : r.getCreatedAt().toString()
        );
    }

    // ---------------------------------------------------------------- delete
    public void deleteCourseContent(Long courseId) {
        List<Review> reviews = reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
        reviewRepository.deleteAll(reviews);

        List<ForumThread> threads = forumRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
        forumRepository.deleteAll(threads);

        List<LectureNote> notes = noteRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
        noteRepository.deleteAll(notes);

        List<LessonMaterial> materials = materialRepository.findByCourseIdOrderByCreatedAtAsc(courseId);
        materialRepository.deleteAll(materials);
    }

    // ---------------------------------------------------------------- forum
    public ForumThread addThread(Long courseId, String authorId, String authorName,
                                 String title, String body) {
        ForumThread t = new ForumThread();
        t.setCourseId(courseId);
        t.setAuthorId(authorId);
        t.setAuthorName(authorName);
        t.setTitle(title);
        t.setBody(body);
        t.setCreatedAt(Instant.now());
        return forumRepository.save(t);
    }

    public ForumThread addPost(String threadId, String authorId, String authorName,
                               boolean isInstructor, String body) {
        ForumThread t = forumRepository.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("Thread not found"));
        ForumPost p = new ForumPost();
        p.setId(java.util.UUID.randomUUID().toString());
        p.setAuthorId(authorId);
        p.setAuthorName(authorName);
        p.setBody(body);
        p.setInstructor(isInstructor);
        p.setCreatedAt(Instant.now());
        t.getPosts().add(p);
        return forumRepository.save(t);
    }

    public List<Map<String, Object>> forumForCourse(Long courseId) {
        return forumRepository.findByCourseIdOrderByCreatedAtDesc(courseId).stream()
                .map(this::toThreadView)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toThreadView(ForumThread t) {
        List<Map<String, Object>> posts = new ArrayList<>();
        for (ForumPost p : t.getPosts()) {
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("id", p.getId());
            pm.put("authorName", p.getAuthorName() == null ? "" : p.getAuthorName());
            pm.put("body", p.getBody() == null ? "" : p.getBody());
            pm.put("isInstructor", p.isInstructor());
            pm.put("createdAt", p.getCreatedAt() == null ? "" : p.getCreatedAt().toString());
            posts.add(pm);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("authorName", t.getAuthorName() == null ? "" : t.getAuthorName());
        m.put("title", t.getTitle() == null ? "" : t.getTitle());
        m.put("body", t.getBody() == null ? "" : t.getBody());
        m.put("solved", t.isSolved());
        m.put("createdAt", t.getCreatedAt() == null ? "" : t.getCreatedAt().toString());
        m.put("posts", posts);
        return m;
    }
}
