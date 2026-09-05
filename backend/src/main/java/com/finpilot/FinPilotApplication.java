package com.finpilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for FinPilot AI Backend.
 * 
 * @SpringBootApplication enables:
 *  1. @Configuration - Tags the class as a source of bean definitions.
 *  2. @EnableAutoConfiguration - Tells Spring Boot to start adding beans based on classpath settings.
 *  3. @ComponentScan - Tells Spring to look for other components, configurations, and services in the 'com.finpilot' package.
 */
@SpringBootApplication
public class FinPilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinPilotApplication.class, args);
    }
}
