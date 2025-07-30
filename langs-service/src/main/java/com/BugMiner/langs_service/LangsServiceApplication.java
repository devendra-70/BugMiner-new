package com.BugMiner.langs_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LangsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(LangsServiceApplication.class, args);
	}

}
