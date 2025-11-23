package com.exchange.currency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Currency Exchange Rates Provider Service.
 * Aggregates exchange rates from multiple providers and exposes REST API.
 */
@SpringBootApplication
@EnableScheduling
public final class CurrencyExchangeRatesApplication {

    private CurrencyExchangeRatesApplication() {
        // Private constructor to prevent instantiation
    }

    public static void main(String[] args) {
        SpringApplication.run(CurrencyExchangeRatesApplication.class, args);
    }
}
