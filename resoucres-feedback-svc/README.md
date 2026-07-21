# CourseConnect resources & feedback service

This Spring Boot service owns the MongoDB data that is not a good fit for the relational course and identity services:

- `course_resources`: lecture notes, multimedia and supplementary resources. The `content` object is intentionally open-ended for rich-text blocks, embed settings, attachment metadata, or future formats.
- `course_reviews`: one rating/review per student and course. Optional survey fields belong in the flexible `attributes` object.
- `discussion_threads`: questions, topics, tags and embedded replies. Thread/post metadata remains schema-flexible for votes, moderation or attachments.

## Run locally

Start MongoDB, then run the service from this directory. If MongoDB is running
with `--auth`, create the least-privilege application account once, using an
existing MongoDB administrator account:

```bash
mongosh --authenticationDatabase admin -u <admin-user> -p admin scripts/create-courseconnect-user.js
```

Then provide the password you chose through an environment variable:

```bash
export MONGODB_URI='mongodb://courseconnect_app:URL_ENCODED_PASSWORD@localhost:27017/courseconnect_content?authSource=admin'
export JWT_SECRET='the-same-secret-used-by-aut-and-course-services'
./mvnw spring-boot:run
```

The service listens on `8083`. The static frontend calls it directly through `BACKEND_CONTENT_API_URL`; the API gateway also exposes it under `/resources-feedback-svc/**` when Eureka is running.

## API examples

All write requests require a JWT from `aut-svc` in `Authorization: Bearer <token>`.

```bash
# flexible lecture note / multimedia / resource payload
curl -X POST http://localhost:8083/api/content/courses/42/resources \
  -H 'Authorization: Bearer <lecturer-token>' -H 'Content-Type: application/json' \
  -d '{"kind":"LECTURE_NOTE","title":"Week 1 slides","content":{"blocks":[{"type":"markdown","text":"# Welcome"}],"downloadable":true}}'

# a student review
curl -X POST http://localhost:8083/api/community/courses/42/reviews \
  -H 'Authorization: Bearer <student-token>' -H 'Content-Type: application/json' \
  -d '{"rating":5,"title":"Very practical","feedback":"The examples made the API concepts click.","attributes":{"pace":"good"}}'

# forum question and an answer/reply
curl -X POST http://localhost:8083/api/community/courses/42/threads \
  -H 'Authorization: Bearer <token>' -H 'Content-Type: application/json' \
  -d '{"title":"How should I test this endpoint?","body":"I need help with the week 2 exercise.","tags":["testing","week-2"]}'
```

## Required MongoDB queries

The application executes these queries in `ReviewService` and `DiscussionService`:

```javascript
// All feedback for a course
db.course_reviews.find({ courseId: "42" }).sort({ createdAt: -1 })

// Top-rated courses, aggregated from student reviews
db.course_reviews.aggregate([
  { $group: { _id: "$courseId", averageRating: { $avg: "$rating" }, reviewCount: { $sum: 1 } } },
  { $sort: { averageRating: -1, reviewCount: -1 } },
  { $limit: 10 }
])

// Keyword/topic search using the text index created from title, body and tags
db.discussion_threads.find({ $text: { $search: "testing endpoint" }, courseId: "42" }, { score: { $meta: "textScore" } })
  .sort({ score: { $meta: "textScore" } })
```

Useful read endpoints:

- `GET /api/content/courses/{courseId}/resources`
- `GET /api/community/courses/{courseId}/reviews`
- `GET /api/community/courses/top-rated?limit=10`
- `GET /api/community/courses/{courseId}/threads?q=keyword`
