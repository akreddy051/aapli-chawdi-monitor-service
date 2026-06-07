package com.example.aapliChawdi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AapliChawdiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AapliChawdiApplication.class, args);
	}

}
