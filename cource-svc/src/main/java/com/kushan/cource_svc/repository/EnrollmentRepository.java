package com.kushan.cource_svc.repository;

import com.kushan.cource_svc.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentId(String studentId);

    Optional<Enrollment> findByStudentIdAndCourseId(String studentId, Long courseId);

    long countByCourseId(Long courseId);

    List<Enrollment> findByCourseId(Long courseId);
}
