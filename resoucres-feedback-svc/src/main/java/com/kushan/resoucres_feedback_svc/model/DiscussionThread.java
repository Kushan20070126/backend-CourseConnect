package com.kushan.resoucres_feedback_svc.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Document("discussion_threads")
@CompoundIndex(name = "course_recent", def = "{'courseId': 1, 'updatedAt': -1}")
public class DiscussionThread {
    @Id private String id;
    private String courseId;
    @TextIndexed(weight = 4) private String title;
    @TextIndexed(weight = 2) private String body;
    @TextIndexed private List<String> tags = new ArrayList<>();
    private String authorId;
    private String authorName;
    private List<ForumPost> posts = new ArrayList<>();
    private Map<String, Object> metadata = new LinkedHashMap<>();
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    public static class ForumPost {
        private String id;
        private String authorId;
        private String authorName;
        private String body;
        private boolean answer;
        private Map<String, Object> metadata = new LinkedHashMap<>();
        private Instant createdAt = Instant.now();
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getAuthorId() { return authorId; }
        public void setAuthorId(String authorId) { this.authorId = authorId; }
        public String getAuthorName() { return authorName; }
        public void setAuthorName(String authorName) { this.authorName = authorName; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        public boolean isAnswer() { return answer; }
        public void setAnswer(boolean answer) { this.answer = answer; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata == null ? new LinkedHashMap<>() : metadata; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    }

    public String getId() { return id; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags == null ? new ArrayList<>() : tags; }
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public List<ForumPost> getPosts() { return posts; }
    public void setPosts(List<ForumPost> posts) { this.posts = posts == null ? new ArrayList<>() : posts; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata == null ? new LinkedHashMap<>() : metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
