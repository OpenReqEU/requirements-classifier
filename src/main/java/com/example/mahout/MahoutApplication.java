package com.example.mahout;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class MahoutApplication {

	@Value("${base}")
	private String baseHost;

	public static void main(String[] args) {
		SpringApplication.run(MahoutApplication.class, args);
		}

}
