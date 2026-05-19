package com.vektor.mail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ComponentScan(basePackages = "com.vektor.mail")
@EntityScan(basePackages = "com.vektor.mail")
@EnableJpaRepositories(basePackages = "com.vektor.mail")
@EnableAsync
public class VektorMailApplication {
    public static void main(String[] args) {
        SpringApplication.run(VektorMailApplication.class, args);
    }
}
