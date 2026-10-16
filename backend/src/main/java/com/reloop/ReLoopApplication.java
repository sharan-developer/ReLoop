package com.reloop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ReLoopApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReLoopApplication.class, args);
    }
}
