package com.example.mahout;

import com.example.mahout.storage.StorageProperties;
import com.example.mahout.storage.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.paths.RelativePathProvider;
import springfox.documentation.spring.web.plugins.Docket;

import javax.servlet.ServletContext;


@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class MahoutApplication {

	@Value("${base}")
	private String baseHost;

	public static void main(String[] args) {
		SpringApplication.run(MahoutApplication.class, args);
		}

	@Bean
	CommandLineRunner init(StorageService storageService) {
		return (args) -> {
			storageService.deleteAll();
			storageService.init();
		};
	}

}
