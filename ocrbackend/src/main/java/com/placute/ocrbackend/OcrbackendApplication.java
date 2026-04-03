package com.placute.ocrbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableMethodSecurity
@EnableAsync
@SpringBootApplication
public class OcrbackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(OcrbackendApplication.class, args);
	}

}
