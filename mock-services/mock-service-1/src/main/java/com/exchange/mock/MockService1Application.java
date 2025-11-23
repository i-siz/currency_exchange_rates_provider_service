package com.exchange.mock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for Mock Exchange Rate Service 1.
 */
@SpringBootApplication
public final class MockService1Application {

    private MockService1Application() {
        // Private constructor to prevent instantiation
    }

    public static void main(String[] args) {
        SpringApplication.run(MockService1Application.class, args);
    }
}
