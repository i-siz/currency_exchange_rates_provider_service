package com.exchange.mock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for Mock Exchange Rate Service 2.
 */
@SpringBootApplication
public final class MockService2Application {

    private MockService2Application() {
        // Private constructor to prevent instantiation
    }

    public static void main(String[] args) {
        SpringApplication.run(MockService2Application.class, args);
    }
}
