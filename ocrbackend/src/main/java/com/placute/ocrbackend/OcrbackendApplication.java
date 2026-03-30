package com.placute.ocrbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@EnableMethodSecurity
@SpringBootApplication
public class OcrbackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(OcrbackendApplication.class, args);
	}

}
