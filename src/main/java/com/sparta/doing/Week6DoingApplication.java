package com.sparta.doing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class Week6DoingApplication {

    public static void main(String[] args) {
        SpringApplication.run(Week6DoingApplication.class, args);
    }

}
