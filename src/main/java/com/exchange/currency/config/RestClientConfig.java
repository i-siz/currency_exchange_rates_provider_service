package com.exchange.currency.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for REST client beans.
 */
@Configuration
public class RestClientConfig {

    /**
     * RestTemplate bean for HTTP client operations.
     * Used by external API providers to fetch exchange rates.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
