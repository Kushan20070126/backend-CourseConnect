package com.kushan.resoucres_feedback_svc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// The context test must not create indexes in a developer's secured MongoDB.
// Integration tests should supply their own authenticated MONGODB_URI.
@SpringBootTest(properties = "spring.data.mongodb.auto-index-creation=false")
class ResoucresFeedbackSvcApplicationTests {

	@Test
	void contextLoads() {
	}

}
