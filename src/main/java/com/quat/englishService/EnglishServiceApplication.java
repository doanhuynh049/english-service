package com.quat.englishService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EnglishServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnglishServiceApplication.class, args);
    }
}
