package com.kevin.demo.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TaskWorkflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskWorkflowApplication.class, args);
	}

}
