package com.kushan.cource_svc.service;

import com.kushan.cource_svc.model.Course;
import com.kushan.cource_svc.model.Enrollment;
import com.kushan.cource_svc.model.Lesson;
import com.kushan.cource_svc.model.Section;
import com.kushan.cource_svc.repository.CourseRepository;
import com.kushan.cource_svc.repository.EnrollmentRepository;
import com.kushan.cource_svc.repository.PaymentRepository;
import com.kushan.cource_svc.model.Payment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EnrollmentService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;
    private final StripeService stripeService;
    private final MinioService minioService;
    private final MongoContentService contentService;

    public EnrollmentService(CourseRepository courseRepository,
                             EnrollmentRepository enrollmentRepository,
                             PaymentRepository paymentRepository,
                             StripeService stripeService,
                             MinioService minioService,
                             MongoContentService contentService) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.paymentRepository = paymentRepository;
        this.stripeService = stripeService;
        this.minioService = minioService;
        this.contentService = contentService;
    }

    // ---------------------------------------------------------------- enroll
    @Transactional
    public Map<String, Object> enroll(Long courseId, String studentId, String studentName) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        boolean free = course.getPrice() == null || course.getPrice() <= 0;

        Optional<Enrollment> existing = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId);
        if (existing.isPresent()) {
            Enrollment e = existing.get();
            if ("ACTIVE".equals(e.getStatus()) || "COMPLETED".equals(e.getStatus())) {
                return Map.of("status", "active", "enrollmentId", e.getId());
            }
            // PENDING -> recover the checkout so the user can finish paying instead of being stuck.
            if (!free) {
                Payment payment = paymentRepository
                        .findByCourseIdAndStudentId(courseId, studentId)
                        .orElse(null);
                String url = payment != null ? stripeService.retrieveCheckoutUrl(payment.getStripeSessionId()) : null;
                if (url == null) {
                    if (payment == null) {
                        payment = new Payment();
                        payment.setCourseId(courseId);
                        payment.setStudentId(studentId);
                        long cents = Math.round(course.getPrice() * 100);
                        payment.setAmountCents((int) cents);
                        payment.setCurrency(course.getCurrency() == null ? "USD" : course.getCurrency());
                        payment.setStatus("CREATED");
                        payment = paymentRepository.save(payment);
                    }
                    StripeService.CheckoutResult checkout =
                            stripeService.createCheckout(course, studentId, payment.getId(), courseId);
                    payment.setStripeSessionId(checkout.sessionId());
                    paymentRepository.save(payment);
                    url = checkout.url();
                }
                return Map.of(
                        "status", "checkout",
                        "enrollmentId", e.getId(),
                        "paymentId", payment != null ? payment.getId() : null,
                        "sessionId", payment != null ? payment.getStripeSessionId() : null,
                        "checkoutUrl", url
                );
            }
            return Map.of("status", "active", "enrollmentId", e.getId());
        }

        if (free) {
            Enrollment e = new Enrollment();
            e.setCourse(course);
            e.setStudentId(studentId);
            e.setStudentName(studentName);
            e.setStatus("ACTIVE");
            enrollmentRepository.save(e);
            bumpStudents(course);
            return Map.of("status", "active", "enrollmentId", e.getId());
        }

        // Paid -> create a PENDING enrollment + a Stripe checkout session.
        Payment payment = new Payment();
        payment.setCourseId(courseId);
        payment.setStudentId(studentId);
        long cents = Math.round(course.getPrice() * 100);
        payment.setAmountCents((int) cents);
        payment.setCurrency(course.getCurrency() == null ? "USD" : course.getCurrency());
        payment.setStatus("CREATED");
        payment = paymentRepository.save(payment);

        Enrollment e = new Enrollment();
        e.setCourse(course);
        e.setStudentId(studentId);
        e.setStudentName(studentName);
        e.setStatus("PENDING");
        enrollmentRepository.save(e);

        StripeService.CheckoutResult checkout =
                stripeService.createCheckout(course, studentId, payment.getId(), courseId);

        payment.setStripeSessionId(checkout.sessionId());
        paymentRepository.save(payment);

        return Map.of(
                "status", "checkout",
                "enrollmentId", e.getId(),
                "paymentId", payment.getId(),
                "sessionId", checkout.sessionId(),
                "checkoutUrl", checkout.url()
        );
    }

    // ---------------------------------------------------------------- my courses
    @Transactional(readOnly = true)
    public List<Map<String, Object>> myCourses(String studentId) {
        return enrollmentRepository.findByStudentId(studentId).stream().map(e -> {
            Course c = e.getCourse();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("enrollmentId", e.getId());
            m.put("status", e.getStatus());
            m.put("progressPercent", e.getProgressPercent());
            m.put("completed", "COMPLETED".equals(e.getStatus()));
            m.put("id", c.getId());
            m.put("title", c.getTitle());
            m.put("summary", c.getSummary());
            m.put("category", c.getCategory());
            m.put("level", c.getLevel());
            m.put("price", c.getPrice());
            m.put("thumbnailUrl", minioService.viewUrl(c.getThumbnailUrl()));
            m.put("instructorName", c.getInstructorName());
            m.put("durationMinutes", c.getDurationMinutes());
            return m;
        }).collect(Collectors.toList());
    }

    // ---------------------------------------------------------------- learn
    @Transactional(readOnly = true)
    public Map<String, Object> learn(Long courseId, String studentId) {
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not enrolled in this course"));
        if (!("ACTIVE".equals(enrollment.getStatus()) || "COMPLETED".equals(enrollment.getStatus()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enrollment is not active yet");
        }
        Course course = enrollment.getCourse();
        Set<String> done = new HashSet<>(Arrays.asList(
                (enrollment.getCompletedLessonIds() == null ? "" : enrollment.getCompletedLessonIds()).split(",")))
                .stream().filter(s -> !s.isBlank()).collect(Collectors.toSet());

        List<Map<String, Object>> sections = new ArrayList<>();
        for (Section sec : course.getSections()) {
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("id", sec.getId());
            sm.put("title", sec.getTitle());
            List<Map<String, Object>> lessons = new ArrayList<>();
            for (Lesson l : sec.getLessons()) {
                Map<String, Object> lm = new LinkedHashMap<>();
                lm.put("id", l.getId());
                lm.put("title", l.getTitle());
                lm.put("description", l.getDescription());
                lm.put("durationMinutes", l.getDurationMinutes());
                lm.put("preview", l.isPreview());
                lm.put("completed", done.contains(String.valueOf(l.getId())));
                lm.put("videoUrl", minioService.viewUrl(l.getVideoUrl()));
                lm.put("materials", contentService.materialsForLesson(l.getId()));
                lm.put("notes", contentService.notesForLesson(l.getId()));
                lessons.add(lm);
            }
            sm.put("lessons", lessons);
            sections.add(sm);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("courseId", course.getId());
        out.put("title", course.getTitle());
        out.put("status", enrollment.getStatus());
        out.put("progressPercent", enrollment.getProgressPercent());
        out.put("completed", "COMPLETED".equals(enrollment.getStatus()));
        out.put("badge", "COMPLETED".equals(enrollment.getStatus()));
        out.put("sections", sections);
        return out;
    }

    // ---------------------------------------------------------------- complete lesson
    @Transactional
    public Map<String, Object> completeLesson(Long courseId, Long lessonId, String studentId) {
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not enrolled"));
        if (!"ACTIVE".equals(enrollment.getStatus()) && !"COMPLETED".equals(enrollment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enrollment not active");
        }
        Course course = enrollment.getCourse();
        int total = course.getSections().stream().mapToInt(s -> s.getLessons().size()).sum();
        if (total == 0) total = 1;

        Set<String> done = new HashSet<>(Arrays.asList(
                (enrollment.getCompletedLessonIds() == null ? "" : enrollment.getCompletedLessonIds()).split(",")))
                .stream().filter(s -> !s.isBlank()).collect(Collectors.toSet());
        done.add(String.valueOf(lessonId));

        enrollment.setCompletedLessonIds(String.join(",", done));
        enrollment.setCompletedLessons(done.size());
        int pct = (int) Math.round((done.size() * 100.0) / total);
        enrollment.setProgressPercent(pct);
        if (pct >= 100) {
            enrollment.setStatus("COMPLETED");
            enrollment.setCompletedAt(java.time.Instant.now());
        }
        enrollmentRepository.save(enrollment);

        return Map.of(
                "progressPercent", pct,
                "completed", pct >= 100,
                "badge", pct >= 100,
                "status", enrollment.getStatus()
        );
    }

    private void bumpStudents(Course course) {
        course.setStudentsCount((course.getStudentsCount() == null ? 0 : course.getStudentsCount()) + 1);
        courseRepository.save(course);
    }
}
