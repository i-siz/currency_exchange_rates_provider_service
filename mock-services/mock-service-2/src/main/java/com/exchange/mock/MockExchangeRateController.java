package com.exchange.mock;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * REST controller that provides mock exchange rates.
 * Generates random rates with small variance around realistic base values.
 * Uses slightly different variance than service 1 to simulate different providers.
 */
@Slf4j
@RestController
public class MockExchangeRateController {

    private static final String[] SUPPORTED_CURRENCIES = {
            "USD", "EUR", "GBP", "JPY", "CHF", "AUD", "CAD", "CNY", "INR", "BRL"
    };

    private static final Map<String, BigDecimal> BASE_RATES = new HashMap<>();
    
    static {
        // Base rates relative to USD (slightly different from service 1)
        BASE_RATES.put("USD", BigDecimal.ONE);
        BASE_RATES.put("EUR", new BigDecimal("0.91"));
        BASE_RATES.put("GBP", new BigDecimal("0.78"));
        BASE_RATES.put("JPY", new BigDecimal("150.20"));
        BASE_RATES.put("CHF", new BigDecimal("0.87"));
        BASE_RATES.put("AUD", new BigDecimal("1.53"));
        BASE_RATES.put("CAD", new BigDecimal("1.37"));
        BASE_RATES.put("CNY", new BigDecimal("7.26"));
        BASE_RATES.put("INR", new BigDecimal("83.25"));
        BASE_RATES.put("BRL", new BigDecimal("4.98"));
    }

    private final Random random = new Random();

    /**
     * Get exchange rates for a base currency.
     * Generates random rates with ±3% variance from base values.
     *
     * @param base base currency code
     * @return exchange rates response
     */
    @GetMapping("/rates")
    public ExchangeRatesResponse getRates(@RequestParam(defaultValue = "USD") String base) {
        log.info("Mock Service 2: Generating rates for base currency: {}", base);

        Map<String, BigDecimal> rates = new HashMap<>();
        BigDecimal baseRate = BASE_RATES.getOrDefault(base, BigDecimal.ONE);

        for (String targetCurrency : SUPPORTED_CURRENCIES) {
            if (!targetCurrency.equals(base)) {
                BigDecimal targetRate = BASE_RATES.getOrDefault(targetCurrency, BigDecimal.ONE);
                BigDecimal rate = targetRate.divide(baseRate, 6, RoundingMode.HALF_UP);
                
                // Add ±3% random variance (slightly more than service 1)
                double variance = 0.97 + (random.nextDouble() * 0.06); // 0.97 to 1.03
                rate = rate.multiply(BigDecimal.valueOf(variance))
                        .setScale(6, RoundingMode.HALF_UP);
                
                rates.put(targetCurrency, rate);
            }
        }

        return new ExchangeRatesResponse(base, rates, System.currentTimeMillis());
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "mock-service-2");
    }

    /**
     * Response model for exchange rates.
     */
    @Data
    @AllArgsConstructor
    public static class ExchangeRatesResponse {
        @JsonProperty("base")
        private String base;

        @JsonProperty("rates")
        private Map<String, BigDecimal> rates;

        @JsonProperty("timestamp")
        private Long timestamp;
    }
}
