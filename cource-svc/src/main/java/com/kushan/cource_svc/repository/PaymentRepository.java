package com.kushan.cource_svc.repository;

import com.kushan.cource_svc.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByStripeSessionId(String sessionId);

    Optional<Payment> findByCourseIdAndStudentId(Long courseId, String studentId);
}
