package com.kushan.cource_svc.repository;

import com.kushan.cource_svc.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByStatus(String status);

    List<Course> findByInstructorId(String instructorId);

    List<Course> findByCategoryIgnoreCase(String category);

    long countByInstructorId(String instructorId);

    Optional<Course> findByIdAndInstructorId(Long id, String instructorId);
}
