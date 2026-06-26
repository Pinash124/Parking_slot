package com.smartparking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EntityScan(basePackages = {"com.smartparking.model.schemas", "com.example.pricing_calculation.domain"})
@EnableJpaRepositories(basePackages = {"com.smartparking.repository", "com.example.pricing_calculation.repository"})
@SpringBootApplication(scanBasePackages = {"com.smartparking", "com.example.pricing_calculation"})
public class SmartParkingApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartParkingApplication.class, args);
	}

}
