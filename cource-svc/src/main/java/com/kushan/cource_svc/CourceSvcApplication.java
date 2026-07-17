package com.kushan.cource_svc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class CourceSvcApplication {

	public static void main(String[] args) {
		SpringApplication.run(CourceSvcApplication.class, args);
	}
}
