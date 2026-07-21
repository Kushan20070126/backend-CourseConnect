package com.kushan.cource_svc.model.mongo;

import java.time.Instant;

/**
 * A single reply within a {@link ForumThread}. Kept as an embedded document so
 * threads carry their full conversation in one MongoDB document.
 */
public class ForumPost {

    private String id;
    private String authorId; // email
    private String authorName;
    private String body;
    private boolean isInstructor = false;
    private Instant createdAt = Instant.now();

    public ForumPost() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public boolean isInstructor() { return isInstructor; }
    public void setInstructor(boolean instructor) { isInstructor = instructor; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
