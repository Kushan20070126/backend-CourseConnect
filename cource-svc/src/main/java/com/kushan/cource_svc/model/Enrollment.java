package com.kushan.cource_svc.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "cc_enrollments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "student_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private String studentId;   // student email
    private String studentName;

    // PENDING (awaiting payment) | ACTIVE | COMPLETED (badge earned)
    @Column(nullable = false)
    private String status = "PENDING";

    private Integer completedLessons = 0;
    private Integer progressPercent = 0;

    @Column(length = 4000)
    private String completedLessonIds = "";   // comma-separated Lesson PKs

    @Column(nullable = false, updatable = false)
    private Instant enrolledAt = Instant.now();

    private Instant completedAt;
}
