package de.vektor.mail.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "de.vektor.mail")
public class VektorMailApplication {

    public static void main(String[] args) {
        SpringApplication.run(VektorMailApplication.class, args);
    }
}
