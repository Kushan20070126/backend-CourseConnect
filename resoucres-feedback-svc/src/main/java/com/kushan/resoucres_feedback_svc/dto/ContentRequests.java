package com.kushan.resoucres_feedback_svc.dto;

import java.util.List;
import java.util.Map;

public final class ContentRequests {
    private ContentRequests() { }
    public record ResourceRequest(String kind, String title, String description, String url, String mimeType,
                                  Map<String, Object> content) { }
    public record ReviewRequest(Integer rating, String title, String feedback, Map<String, Object> attributes) { }
    public record ThreadRequest(String title, String body, List<String> tags, Map<String, Object> metadata) { }
    public record PostRequest(String body, Boolean answer, Map<String, Object> metadata) { }
}
