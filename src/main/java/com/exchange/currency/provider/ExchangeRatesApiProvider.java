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
 * Exchange rate provider using ExchangeRatesAPI.io.
 * Provides free exchange rates without API key requirement.
 */
@Slf4j
@Component
public class ExchangeRatesApiProvider implements ExchangeRateProvider {

    private static final String PROVIDER_NAME = "exchangeratesapi.io";

    @Value("${exchange.providers.exchangeratesapi.api-url:https://api.exchangeratesapi.io/v1/latest}")
    private String apiUrl;

    @Value("${exchange.providers.exchangeratesapi.api-key:#{null}}")
    private String apiKey;

    @Value("${exchange.providers.exchangeratesapi.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate;

    public ExchangeRatesApiProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public Map<String, ExchangeRateDto> fetchExchangeRates(String baseCurrency) {
        if (!enabled) {
            log.debug("ExchangeRatesAPI provider is disabled");
            throw new ExternalApiException(PROVIDER_NAME, "Provider is not configured");
        }

        try {
            String url = apiKey != null && !apiKey.isBlank()
                    ? String.format("%s?access_key=%s&base=%s", apiUrl, apiKey, baseCurrency)
                    : String.format("%s?base=%s", apiUrl, baseCurrency);

            log.debug("Fetching rates from exchangeratesapi.io for base currency: {}", baseCurrency);
            ExchangeRatesApiResponse response = restTemplate.getForObject(
                    url, ExchangeRatesApiResponse.class);

            if (response == null || !response.isSuccess()) {
                if (log.isErrorEnabled()) {
                    log.error("ExchangeRatesAPI returned error: {}", 
                            response != null ? response.getError() : "null response");
                }
                throw new ExternalApiException(PROVIDER_NAME, "API returned error");
            }

            return convertToExchangeRateDtos(response, baseCurrency);
        } catch (ExternalApiException ex) {
            throw ex;
        } catch (Exception ex) {
            if (log.isErrorEnabled()) {
                log.error("Error fetching rates from exchangeratesapi.io: {}", ex.getMessage(), ex);
            }
            throw new ExternalApiException(PROVIDER_NAME, "Failed to fetch rates", ex);
        }
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }

    private Map<String, ExchangeRateDto> convertToExchangeRateDtos(
            ExchangeRatesApiResponse response, String baseCurrency) {
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
     * Response model for exchangeratesapi.io API.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class ExchangeRatesApiResponse {
        private boolean success;
        
        @JsonProperty("base")
        private String base;
        
        @JsonProperty("rates")
        private Map<String, BigDecimal> rates;
        
        @JsonProperty("error")
        private Map<String, Object> error;
    }
}
