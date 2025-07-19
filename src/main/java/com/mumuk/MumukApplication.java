package com.mumuk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan("com.mumuk.domain")
public class MumukApplication {

	public static void main(String[] args) {
		SpringApplication.run(MumukApplication.class, args);
	}

}