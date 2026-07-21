package com.kushan.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		return builder.routes()
			.route("aut-svc", r -> r.order(-1).path("/aut-svc/**")
				.filters(f -> f.rewritePath("/aut-svc/(?<remaining>.*)", "/${remaining}"))
				.uri("lb://aut-svc"))
			.route("course-svc", r -> r.order(-1).path("/course-svc/**")
				.filters(f -> f.rewritePath("/course-svc/(?<remaining>.*)", "/${remaining}"))
				.uri("lb://course-svc"))
			.route("resources-feedback-svc", r -> r.order(-1).path("/resources-feedback-svc/**")
				.filters(f -> f.rewritePath("/resources-feedback-svc/(?<remaining>.*)", "/${remaining}"))
				.uri("lb://resoucres-feedback-svc"))
			.build();
	}
}
