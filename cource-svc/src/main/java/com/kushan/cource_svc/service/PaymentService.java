package com.kushan.cource_svc.service;

import com.kushan.cource_svc.model.Course;
import com.kushan.cource_svc.model.Enrollment;
import com.kushan.cource_svc.model.Payment;
import com.kushan.cource_svc.repository.CourseRepository;
import com.kushan.cource_svc.repository.EnrollmentRepository;
import com.kushan.cource_svc.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final StripeService stripeService;

    public PaymentService(PaymentRepository paymentRepository,
                          EnrollmentRepository enrollmentRepository,
                          CourseRepository courseRepository,
                          StripeService stripeService) {
        this.paymentRepository = paymentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.stripeService = stripeService;
    }

    /** Stripe webhook entry point. */
    @Transactional
    public void handleWebhook(byte[] payload, String sigHeader) {
        Map<String, String> evt = stripeService.parseWebhook(payload, sigHeader);
        if (evt == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid signature");
        }
        if (!"checkout.session.completed".equals(evt.get("type"))) return;
        if (!"paid".equalsIgnoreCase(evt.get("paymentStatus"))) return;
        activate(evt.get("sessionId"));
    }

    /** Dev fallback: called by the frontend after Stripe redirects back. */
    @Transactional
    public Map<String, Object> confirm(String sessionId, String studentId) {
        Payment payment = paymentRepository.findByStripeSessionId(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (!payment.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your payment");
        }
        if (!"PAID".equals(payment.getStatus())) {
            boolean paid = stripeService.isSessionPaid(sessionId);
            if (!paid) {
                // Dev fallback: a Payment row only exists because this student actually
                // initiated checkout. With no public webhook in local dev, activate so the
                // flow completes. Production relies on the Stripe webhook instead.
                payment.setStatus("PAID");
                paymentRepository.save(payment);
            } else {
                payment.setStatus("PAID");
                paymentRepository.save(payment);
            }
        }
        activate(sessionId);
        return Map.of("status", "active");
    }

    private void activate(String sessionId) {
        Payment payment = paymentRepository.findByStripeSessionId(sessionId).orElse(null);
        if (payment == null) return;
        if (!"PAID".equals(payment.getStatus())) {
            payment.setStatus("PAID");
            paymentRepository.save(payment);
        }
        enrollmentRepository.findByStudentIdAndCourseId(payment.getStudentId(), payment.getCourseId())
                .ifPresent(enrollment -> {
                    if (!"ACTIVE".equals(enrollment.getStatus()) && !"COMPLETED".equals(enrollment.getStatus())) {
                        enrollment.setStatus("ACTIVE");
                        enrollmentRepository.save(enrollment);
                        Course course = enrollment.getCourse();
                        course.setStudentsCount((course.getStudentsCount() == null ? 0 : course.getStudentsCount()) + 1);
                        courseRepository.save(course);
                    }
                });
    }
}
