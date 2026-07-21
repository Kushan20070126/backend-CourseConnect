package com.kushan.cource_svc.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Discussion forum thread for a course (Q&A). A thread has a title/body and a
 * list of replies (posts). Stored in MongoDB for flexible, nested discussion.
 */
@Document(collection = "forum_threads")
public class ForumThread {

    @Id
    private String id;

    private Long courseId;
    private String authorId; // email
    private String authorName;
    private String title;
    private String body;
    private boolean solved = false;
    private Instant createdAt = Instant.now();
    private List<ForumPost> posts = new ArrayList<>();

    public ForumThread() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public boolean isSolved() { return solved; }
    public void setSolved(boolean solved) { this.solved = solved; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public List<ForumPost> getPosts() { return posts; }
    public void setPosts(List<ForumPost> posts) { this.posts = posts; }
}
