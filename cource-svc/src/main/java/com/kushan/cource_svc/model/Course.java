package com.kushan.cource_svc.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cc_courses")
@Getter
@Setter
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 400)
    private String summary;

    @Column(length = 4000)
    private String description;

    private String category;
    private String level;      // Beginner | Intermediate | Advanced
    private String language = "English";

    private Double price = 0.0;   // in major units (e.g. dollars)
    private String currency = "USD";

    private String thumbnailUrl;

    @Column(nullable = false)
    private String instructorId;   // lecturer email (from auth-svc)
    @Column(nullable = false)
    private String instructorName;

    @Column(nullable = false)
    private String status = "DRAFT";   // DRAFT | PUBLISHED

    private Double rating = 0.0;
    private Integer ratingsCount = 0;
    private Integer studentsCount = 0;
    private Integer durationMinutes = 0;

    @Column(length = 4000)
    private String learnOutcomes;   // newline-separated "what you'll learn"

    @Column(length = 4000)
    private String requirements;     // newline-separated prerequisites

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<Section> sections = new ArrayList<>();

    public void addSection(Section s) {
        s.setCourse(this);
        sections.add(s);
    }
}
