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
public class CurrencyExchangeRatesApplication {

    public static void main(String[] args) {
        SpringApplication.run(CurrencyExchangeRatesApplication.class, args);
    }
}
