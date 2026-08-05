package com.leets.k_beauty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class KBeautyApplication {

	public static void main(String[] args) {
		SpringApplication.run(KBeautyApplication.class, args);
	}

}
