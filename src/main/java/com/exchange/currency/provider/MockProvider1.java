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
 * Exchange rate provider using first mock service.
 * Mock service should expose endpoint: GET /rates?base={currency}
 */
@Slf4j
@Component
public class MockProvider1 implements ExchangeRateProvider {

    private static final String PROVIDER_NAME = "mock-service-1";

    @Value("${exchange.providers.mock1.url:http://localhost:8081}")
    private String baseUrl;

    @Value("${exchange.providers.mock1.enabled:true}")
    private boolean enabled;

    private final RestTemplate restTemplate;

    public MockProvider1(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public Map<String, ExchangeRateDto> fetchExchangeRates(String baseCurrency) {
        if (!enabled) {
            log.debug("Mock provider 1 is disabled");
            throw new ExternalApiException(PROVIDER_NAME, "Provider is not configured");
        }

        try {
            String url = String.format("%s/rates?base=%s", baseUrl, baseCurrency);

            log.debug("Fetching rates from mock-service-1 for base currency: {}", baseCurrency);
            MockServiceResponse response = restTemplate.getForObject(url, MockServiceResponse.class);

            if (response == null) {
                if (log.isErrorEnabled()) {
                    log.error("Mock service 1 returned null response");
                }
                throw new ExternalApiException(PROVIDER_NAME, "Returned null response");
            }

            return convertToExchangeRateDtos(response);
        } catch (ExternalApiException ex) {
            throw ex;
        } catch (Exception ex) {
            if (log.isErrorEnabled()) {
                log.error("Error fetching rates from mock-service-1: {}", ex.getMessage(), ex);
            }
            throw new ExternalApiException(PROVIDER_NAME, "Failed to fetch rates", ex);
        }
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }

    private Map<String, ExchangeRateDto> convertToExchangeRateDtos(MockServiceResponse response) {
        Map<String, ExchangeRateDto> result = new HashMap<>();
        LocalDateTime timestamp = LocalDateTime.now();

        response.getRates().forEach((targetCurrency, rate) -> {
            ExchangeRateDto dto = ExchangeRateDto.builder()
                    .baseCurrency(response.getBase())
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
     * Response model for mock service.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class MockServiceResponse {
        @JsonProperty("base")
        private String base;
        
        @JsonProperty("rates")
        private Map<String, BigDecimal> rates;
        
        @JsonProperty("timestamp")
        private Long timestamp;
    }
}
