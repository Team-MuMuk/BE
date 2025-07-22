package com.mumuk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EntityScan("com.mumuk.domain")
@EnableScheduling
public class MumukApplication {

	public static void main(String[] args) {
		SpringApplication.run(MumukApplication.class, args);
	}

}