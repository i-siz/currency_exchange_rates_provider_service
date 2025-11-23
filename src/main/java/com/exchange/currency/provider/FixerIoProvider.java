package com.exchange.currency.provider;

import com.exchange.currency.dto.ExchangeRateDto;
import com.exchange.currency.exception.ExternalApiException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Exchange rate provider using fixer.io API.
 * Requires API key configured in application.yml.
 */
@Slf4j
@Component
public class FixerIoProvider implements ExchangeRateProvider {

    private static final String PROVIDER_NAME = "fixer.io";

    @Value("${exchange.providers.fixer.api-url:http://data.fixer.io/api/latest}")
    private String apiUrl;

    @Value("${exchange.providers.fixer.api-key:#{null}}")
    private String apiKey;

    @Value("${exchange.providers.fixer.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate;

    public FixerIoProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public Map<String, ExchangeRateDto> fetchExchangeRates(String baseCurrency) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            log.debug("Fixer.io provider is disabled or API key not configured");
            throw new ExternalApiException(PROVIDER_NAME, "Provider is not configured");
        }

        try {
            String url = String.format("%s?access_key=%s&base=%s", 
                    apiUrl, apiKey, baseCurrency);

            log.debug("Fetching rates from fixer.io for base currency: {}", baseCurrency);
            FixerIoResponse response = restTemplate.getForObject(url, FixerIoResponse.class);

            if (response == null || !response.isSuccess()) {
                if (log.isErrorEnabled()) {
                    log.error("Fixer.io API returned error: {}", 
                            response != null ? response.getError() : "null response");
                }
                throw new ExternalApiException(PROVIDER_NAME, "API returned error");
            }

            return convertToExchangeRateDtos(response, baseCurrency);
        } catch (ExternalApiException ex) {
            throw ex;
        } catch (Exception ex) {
            if (log.isErrorEnabled()) {
                log.error("Error fetching rates from fixer.io: {}", ex.getMessage(), ex);
            }
            throw new ExternalApiException(PROVIDER_NAME, "Failed to fetch rates", ex);
        }
    }

    @Override
    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    private Map<String, ExchangeRateDto> convertToExchangeRateDtos(
            FixerIoResponse response, String baseCurrency) {
        Map<String, ExchangeRateDto> result = new HashMap<>();
        LocalDateTime timestamp = LocalDateTime.now();

        response.getRates().forEach((targetCurrency, rate) -> {
            ExchangeRateDto dto = ExchangeRateDto.builder()
                    .baseCurrency(baseCurrency)
                    .targetCurrency(targetCurrency)
                    .rate(rate)
                    .provider(PROVIDER_NAME)
                    .timestamp(timestamp)
                    .build();
            result.put(targetCurrency, dto);
        });

        return result;
    }

    /**
     * Response model for fixer.io API.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class FixerIoResponse {
        private boolean success;
        
        @JsonProperty("base")
        private String base;
        
        @JsonProperty("rates")
        private Map<String, BigDecimal> rates;
        
        @JsonProperty("error")
        private FixerIoError error;
    }

    /**
     * Error model for fixer.io API.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class FixerIoError {
        private int code;
        private String type;
        private String info;
    }
}
