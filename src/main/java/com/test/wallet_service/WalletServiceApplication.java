package com.test.wallet_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = "com.test")
@EnableJpaRepositories("com.test.Repositories")  // Для репозиториев
@EntityScan("com.test.Models")
public class WalletServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(WalletServiceApplication.class, args);
	}
}