package com.mumuk;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MumukApplication {

	public static void main(String[] args) {
		SpringApplication.run(MumukApplication.class, args);
	}

	@Bean
	public CommandLineRunner logRawEnv() {
		return args -> {
			System.out.println("🔍 POSTGRESQL_URL (env): " + System.getenv("POSTGRESQL_URL"));
			System.out.println("🔍 SPRING_PROFILES_ACTIVE (env): " + System.getenv("SPRING_PROFILES_ACTIVE"));
		};
	}

}