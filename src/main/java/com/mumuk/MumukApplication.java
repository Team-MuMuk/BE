package com.mumuk;

import org.springframework.beans.factory.annotation.Value;
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
	public CommandLineRunner checkDbUrl(@Value("${spring.datasource.url}") String url) {
		return args -> System.out.println("✅ 실제 DB URL: " + url);
	}

}
