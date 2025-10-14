package com.example.MarketS;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@EnableAsync
public class MarketSApplication {

	public static void main(String[] args) {

        SpringApplication.run(MarketSApplication.class, args);
	}

}
