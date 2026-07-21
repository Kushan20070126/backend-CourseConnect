package com.kushan.resoucres_feedback_svc.service;

import com.kushan.resoucres_feedback_svc.dto.ContentRequests.PostRequest;
import com.kushan.resoucres_feedback_svc.dto.ContentRequests.ThreadRequest;
import com.kushan.resoucres_feedback_svc.model.DiscussionThread;
import com.kushan.resoucres_feedback_svc.repository.DiscussionThreadRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class DiscussionService {
    private final DiscussionThreadRepository threads;
    private final MongoTemplate mongo;
    public DiscussionService(DiscussionThreadRepository threads, MongoTemplate mongo) {
        this.threads = threads;
        this.mongo = mongo;
    }

    /** Query 3: text-index search across title, body, and tags within a course. */
    public List<DiscussionThread> search(String courseId, String keyword) {
        if (keyword == null || keyword.isBlank()) return threads.findByCourseIdOrderByUpdatedAtDesc(courseId);
        TextQuery query = TextQuery.queryText(new org.springframework.data.mongodb.core.query.TextCriteria().matchingAny(keyword.trim().split("\\s+")))
                .sortByScore();
        query.addCriteria(Criteria.where("courseId").is(courseId));
        return mongo.find(query, DiscussionThread.class);
    }

    public DiscussionThread create(String courseId, ThreadRequest request, String author) {
        if (request == null || blank(request.title()) || blank(request.body())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A thread title and question are required");
        }
        DiscussionThread thread = new DiscussionThread();
        thread.setCourseId(courseId);
        thread.setTitle(request.title().trim());
        thread.setBody(request.body().trim());
        thread.setTags(request.tags());
        thread.setMetadata(request.metadata());
        thread.setAuthorId(author);
        thread.setAuthorName(displayName(author));
        return threads.save(thread);
    }

    public DiscussionThread post(String threadId, PostRequest request, String author) {
        if (request == null || blank(request.body())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reply cannot be empty");
        }
        DiscussionThread thread = threads.findById(threadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Discussion thread not found"));
        DiscussionThread.ForumPost post = new DiscussionThread.ForumPost();
        post.setId(UUID.randomUUID().toString());
        post.setAuthorId(author);
        post.setAuthorName(displayName(author));
        post.setBody(request.body().trim());
        post.setAnswer(Boolean.TRUE.equals(request.answer()));
        post.setMetadata(request.metadata());
        thread.getPosts().add(post);
        return threads.save(thread);
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String displayName(String email) { int at = email.indexOf('@'); return at > 0 ? email.substring(0, at) : email; }
}
