package com.kushan.cource_svc.dto;

import java.util.List;

public record CourseRequest(
        String title,
        String summary,
        String description,
        String category,
        String level,
        String language,
        Double price,
        String instructorName,
        List<String> learn,
        List<String> requirements,
        List<SectionRequest> sections
) {
    public record SectionRequest(
            String title,
            List<LessonRequest> lessons
    ) {}

    public record LessonRequest(
            String title,
            String description,
            Integer durationMinutes,
            Boolean preview
    ) {}
}
