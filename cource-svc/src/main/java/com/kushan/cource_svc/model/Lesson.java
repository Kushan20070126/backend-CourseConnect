package com.kushan.cource_svc.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cc_lessons")
@Getter
@Setter
@NoArgsConstructor
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    private Integer durationMinutes = 0;

    // MinIO object key/URL for the lesson video (null until uploaded).
    private String videoUrl;

    private boolean preview = false;

    private Integer position = 0;
}
