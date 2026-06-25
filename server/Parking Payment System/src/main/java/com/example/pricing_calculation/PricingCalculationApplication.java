package com.example.pricing_calculation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PricingCalculationApplication {

	public static void main(String[] args) {
		SpringApplication.run(PricingCalculationApplication.class, args);
	}

}
