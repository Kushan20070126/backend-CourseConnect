package com.kushan.cource_svc.service;

import com.kushan.cource_svc.dto.CourseRequest;
import com.kushan.cource_svc.model.Course;
import com.kushan.cource_svc.model.Enrollment;
import com.kushan.cource_svc.model.Lesson;
import com.kushan.cource_svc.model.Section;
import com.kushan.cource_svc.repository.CourseRepository;
import com.kushan.cource_svc.repository.EnrollmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final MinioService minioService;
    private final MongoContentService contentService;

    public CourseService(CourseRepository courseRepository,
                         EnrollmentRepository enrollmentRepository,
                         MinioService minioService,
                         MongoContentService contentService) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.minioService = minioService;
        this.contentService = contentService;
    }

    // ---------------------------------------------------------------- create
    @Transactional
    public Course createCourse(CourseRequest req, String instructorId, String instructorName) {
        Course course = new Course();
        applyRequest(course, req);
        course.setInstructorId(instructorId);
        course.setInstructorName(req.instructorName() != null && !req.instructorName().isBlank()
                ? req.instructorName() : instructorName);
        course.setStatus("DRAFT");
        return courseRepository.save(course);
    }

    @Transactional
    public Course updateCourse(Long id, CourseRequest req, String instructorId) {
        Course course = ownedCourse(id, instructorId);
        applyRequest(course, req);
        return courseRepository.save(course);
    }

    @Transactional
    public void publish(Long id, String instructorId) {
        Course course = ownedCourse(id, instructorId);
        course.setStatus("PUBLISHED");
        courseRepository.save(course);
    }

    @Transactional
    public void delete(Long id, String instructorId) {
        Course course = ownedCourse(id, instructorId);
        contentService.deleteCourseContent(id);
        List<Enrollment> enrollments = enrollmentRepository.findByCourseId(id);
        enrollmentRepository.deleteAll(enrollments);
        courseRepository.delete(course);
    }

    // ---------------------------------------------------------------- read
    public List<Map<String, Object>> listPublished(String category, String level, String q, String sort) {
        List<Course> courses = courseRepository.findByStatus("PUBLISHED");
        if (category != null && !category.isBlank()) {
            courses = courses.stream().filter(c -> category.equalsIgnoreCase(c.getCategory())).collect(Collectors.toList());
        }
        if (level != null && !level.isBlank()) {
            courses = courses.stream().filter(c -> level.equalsIgnoreCase(c.getLevel())).collect(Collectors.toList());
        }
        if (q != null && !q.isBlank()) {
            String s = q.toLowerCase();
            courses = courses.stream()
                    .filter(c -> (c.getTitle() != null && c.getTitle().toLowerCase().contains(s))
                            || (c.getSummary() != null && c.getSummary().toLowerCase().contains(s))
                            || (c.getCategory() != null && c.getCategory().toLowerCase().contains(s)))
                    .collect(Collectors.toList());
        }
        Comparator<Course> cmp = switch (sort == null ? "" : sort) {
            case "price_asc" -> Comparator.comparing(Course::getPrice, Comparator.nullsLast(Double::compareTo));
            case "price_desc" -> Comparator.comparing(Course::getPrice, Comparator.nullsLast(Double::compareTo)).reversed();
            case "rating" -> Comparator.comparing(Course::getRating, Comparator.nullsLast(Double::compareTo)).reversed();
            case "newest" -> Comparator.comparing(Course::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
            default -> Comparator.comparing(Course::getStudentsCount, Comparator.nullsLast(Integer::compareTo)).reversed();
        };
        courses.sort(cmp);
        return courses.stream().map(this::toSummary).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDetail(Long id, String viewerEmail) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        boolean enrolledActive = viewerEmail != null
                && enrollmentRepository.findByStudentIdAndCourseId(viewerEmail, id)
                    .map(e -> "ACTIVE".equals(e.getStatus()) || "COMPLETED".equals(e.getStatus()))
                    .orElse(false);

        Map<String, Object> out = toSummary(course);
        out.put("description", course.getDescription());
        out.put("learn", splitLines(course.getLearnOutcomes()));
        out.put("requirements", splitLines(course.getRequirements()));
        out.put("enrolled", enrolledActive);
        out.put("paid", course.getPrice() != null && course.getPrice() > 0);
        out.put("reviews", contentService.reviewSummary(id));

        List<Map<String, Object>> sections = new ArrayList<>();
        for (Section sec : course.getSections()) {
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("id", sec.getId());
            sm.put("title", sec.getTitle());
            sm.put("position", sec.getPosition());
            List<Map<String, Object>> lessons = new ArrayList<>();
            for (Lesson l : sec.getLessons()) {
                Map<String, Object> lm = new LinkedHashMap<>();
                lm.put("id", l.getId());
                lm.put("title", l.getTitle());
                lm.put("description", l.getDescription());
                lm.put("durationMinutes", l.getDurationMinutes());
                lm.put("preview", l.isPreview());
                lm.put("position", l.getPosition());
                boolean accessible = l.isPreview() || enrolledActive;
                lm.put("videoUrl", accessible ? minioService.viewUrl(l.getVideoUrl()) : null);
                lessons.add(lm);
            }
            sm.put("lessons", lessons);
            sections.add(sm);
        }
        out.put("sections", sections);
        return out;
    }

    public List<Map<String, Object>> lecturerCourses(String instructorId) {
        return courseRepository.findByInstructorId(instructorId).stream()
                .sorted(Comparator.comparing(Course::getCreatedAt).reversed())
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    public Map<String, Object> lecturerStats(String instructorId) {
        List<Course> mine = courseRepository.findByInstructorId(instructorId);
        int totalCourses = mine.size();
        int totalStudents = mine.stream().mapToInt(c -> c.getStudentsCount() == null ? 0 : c.getStudentsCount()).sum();
        long paidCourses = mine.stream().filter(c -> c.getPrice() != null && c.getPrice() > 0).count();
        // Revenue is tracked via payments; approximate from paid course price * students.
        double revenue = mine.stream()
                .filter(c -> c.getPrice() != null && c.getPrice() > 0)
                .mapToDouble(c -> c.getPrice() * (c.getStudentsCount() == null ? 0 : c.getStudentsCount()))
                .sum();

        List<Map<String, Object>> perCourse = mine.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("title", c.getTitle());
            m.put("students", c.getStudentsCount());
            m.put("price", c.getPrice());
            m.put("status", c.getStatus());
            m.put("rating", c.getRating());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalCourses", totalCourses);
        out.put("totalStudents", totalStudents);
        out.put("paidCourses", paidCourses);
        out.put("revenue", Math.round(revenue * 100.0) / 100.0);
        out.put("courses", perCourse);
        return out;
    }

    // ---------------------------------------------------------------- uploads
    @Transactional
    public String uploadThumbnail(Long id, MultipartFile file, String instructorId) {
        Course course = ownedCourse(id, instructorId);
        String key = minioService.upload("thumbnails", file);
        course.setThumbnailUrl(key);
        courseRepository.save(course);
        return key;
    }

    @Transactional
    public String uploadLessonVideo(Long lessonId, MultipartFile file, String instructorId) {
        Course owner = findCourseByLessonId(lessonId);
        if (!owner.getInstructorId().equals(instructorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your course");
        }
        Lesson lesson = owner.getSections().stream()
                .flatMap(s -> s.getLessons().stream())
                .filter(l -> l.getId().equals(lessonId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
        String key = minioService.upload("videos", file);
        lesson.setVideoUrl(key);
        courseRepository.save(owner);
        return key;
    }

    // ---------------------------------------------------------------- helpers
    private Course ownedCourse(Long id, String instructorId) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        if (!course.getInstructorId().equals(instructorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this course");
        }
        return course;
    }

    private Course findCourseByLessonId(Long lessonId) {
        return courseRepository.findAll().stream()
                .filter(c -> c.getSections().stream()
                        .flatMap(s -> s.getLessons().stream())
                        .anyMatch(l -> l.getId().equals(lessonId)))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    private void applyRequest(Course course, CourseRequest req) {
        course.setTitle(req.title());
        course.setSummary(req.summary());
        course.setDescription(req.description());
        course.setCategory(req.category());
        course.setLevel(req.level());
        course.setLanguage(req.language() != null ? req.language() : "English");
        course.setPrice(req.price() != null ? req.price() : 0.0);
        course.setLearnOutcomes(req.learn() == null ? "" : String.join("\n", req.learn()));
        course.setRequirements(req.requirements() == null ? "" : String.join("\n", req.requirements()));

        course.getSections().clear();
        int si = 0, totalLessons = 0, totalMin = 0;
        if (req.sections() != null) {
            for (CourseRequest.SectionRequest sr : req.sections()) {
                Section section = new Section();
                section.setTitle(sr.title());
                section.setPosition(si++);
                int li = 0;
                if (sr.lessons() != null) {
                    for (CourseRequest.LessonRequest lr : sr.lessons()) {
                        Lesson lesson = new Lesson();
                        lesson.setTitle(lr.title());
                        lesson.setDescription(lr.description());
                        lesson.setDurationMinutes(lr.durationMinutes() != null ? lr.durationMinutes() : 0);
                        lesson.setPreview(lr.preview() != null && lr.preview());
                        lesson.setPosition(li++);
                        section.getLessons().add(lesson);
                        lesson.setSection(section);
                        totalLessons++;
                        totalMin += lesson.getDurationMinutes();
                    }
                }
                course.addSection(section);
            }
        }
        course.setDurationMinutes(totalMin);
        // store lessonsCount implicitly via sections; keep durationMinutes
        course.setStudentsCount(course.getStudentsCount() == null ? 0 : course.getStudentsCount());
    }

    private Map<String, Object> toSummary(Course c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("title", c.getTitle());
        m.put("summary", c.getSummary());
        m.put("category", c.getCategory());
        m.put("level", c.getLevel());
        m.put("price", c.getPrice());
        m.put("currency", c.getCurrency());
        m.put("thumbnailUrl", minioService.viewUrl(c.getThumbnailUrl()));
        m.put("instructorName", c.getInstructorName());
        m.put("instructorId", c.getInstructorId());
        m.put("rating", c.getRating());
        m.put("ratingsCount", c.getRatingsCount());
        m.put("studentsCount", c.getStudentsCount());
        m.put("durationMinutes", c.getDurationMinutes());
        m.put("status", c.getStatus());
        m.put("createdAt", c.getCreatedAt() == null ? null : c.getCreatedAt().toString());
        return m;
    }

    private List<String> splitLines(String s) {
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.split("\n")).map(String::trim).filter(t -> !t.isEmpty()).collect(Collectors.toList());
    }
}
