package com.exchange.currency.provider;

import com.exchange.currency.dto.ExchangeRateDto;
import com.exchange.currency.exception.ExternalApiException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for MockProvider2.
 */
@DisplayName("MockProvider2 Unit Tests")
class MockProvider2Test {

    private WireMockServer wireMockServer;
    private MockProvider2 provider;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        provider = new MockProvider2(new RestTemplate());
        ReflectionTestUtils.setField(provider, "baseUrl", "http://localhost:" + wireMockServer.port());
        ReflectionTestUtils.setField(provider, "enabled", true);
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @Test
    @DisplayName("getProviderName should return mock-service-2")
    void getProviderName_ShouldReturnCorrectName() {
        // When
        String name = provider.getProviderName();

        // Then
        assertThat(name).isNotNull();
        assertThat(name).isNotEmpty();
        assertThat(name).isEqualTo("mock-service-2");
    }

    @Test
    @DisplayName("isAvailable should return true when enabled")
    void isAvailable_ShouldReturnTrue_WhenEnabled() {
        // When
        boolean available = provider.isAvailable();

        // Then
        assertThat(available).isTrue();
    }

    @Test
    @DisplayName("isAvailable should return false when disabled")
    void isAvailable_ShouldReturnFalse_WhenDisabled() {
        // Given
        ReflectionTestUtils.setField(provider, "enabled", false);

        // When
        boolean available = provider.isAvailable();

        // Then
        assertThat(available).isFalse();
    }

    @Test
    @DisplayName("fetchExchangeRates should throw exception when disabled")
    void fetchExchangeRates_ShouldThrowException_WhenDisabled() {
        // Given
        ReflectionTestUtils.setField(provider, "enabled", false);

        // When/Then
        assertThatThrownBy(() -> provider.fetchExchangeRates("USD"))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Provider is not configured");
    }

    @Test
    @DisplayName("fetchExchangeRates should return rates when successful")
    void fetchExchangeRates_ShouldReturnRates_WhenSuccessful() {
        // Given
        stubFor(get(urlEqualTo("/rates?base=USD"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"base\":\"USD\",\"rates\":{\"EUR\":0.91,\"GBP\":0.78},\"timestamp\":1234567890}")));

        // When
        Map<String, ExchangeRateDto> rates = provider.fetchExchangeRates("USD");

        // Then
        assertThat(rates).isNotNull();
        assertThat(rates).isNotEmpty();
        assertThat(rates).hasSize(2);
        assertThat(rates).containsKeys("EUR", "GBP");

        ExchangeRateDto eurRate = rates.get("EUR");
        assertThat(eurRate).isNotNull();
        assertThat(eurRate.getBaseCurrency()).isEqualTo("USD");
        assertThat(eurRate.getTargetCurrency()).isEqualTo("EUR");
        assertThat(eurRate.getRate()).isEqualByComparingTo("0.91");
        assertThat(eurRate.getProvider()).isEqualTo("mock-service-2");
        assertThat(eurRate.getTimestamp()).isNotNull();

        ExchangeRateDto gbpRate = rates.get("GBP");
        assertThat(gbpRate).isNotNull();
        assertThat(gbpRate.getRate()).isEqualByComparingTo("0.78");
    }

    @Test
    @DisplayName("fetchExchangeRates should return empty map when no rates")
    void fetchExchangeRates_ShouldReturnEmptyMap_WhenNoRates() {
        // Given
        stubFor(get(urlEqualTo("/rates?base=USD"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"base\":\"USD\",\"rates\":{},\"timestamp\":1234567890}")));

        // When
        Map<String, ExchangeRateDto> rates = provider.fetchExchangeRates("USD");

        // Then
        assertThat(rates).isNotNull();
        assertThat(rates).isEmpty();
    }

    @Test
    @DisplayName("fetchExchangeRates should throw exception when service returns error")
    void fetchExchangeRates_ShouldThrowException_WhenServiceError() {
        // Given
        stubFor(get(urlEqualTo("/rates?base=USD"))
                .willReturn(aResponse()
                        .withStatus(500)));

        // When/Then
        assertThatThrownBy(() -> provider.fetchExchangeRates("USD"))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Failed to fetch rates");
    }

    @Test
    @DisplayName("fetchExchangeRates should handle single currency rate")
    void fetchExchangeRates_ShouldHandle_SingleCurrency() {
        // Given
        stubFor(get(urlEqualTo("/rates?base=EUR"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"base\":\"EUR\",\"rates\":{\"USD\":1.09},\"timestamp\":1234567890}")));

        // When
        Map<String, ExchangeRateDto> rates = provider.fetchExchangeRates("EUR");

        // Then
        assertThat(rates).hasSize(1);
        assertThat(rates).containsKey("USD");
    }

    @Test
    @DisplayName("fetchExchangeRates should handle multiple currencies")
    void fetchExchangeRates_ShouldHandle_MultipleCurrencies() {
        // Given
        stubFor(get(urlEqualTo("/rates?base=USD"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"base\":\"USD\",\"rates\":{\"EUR\":0.91,\"GBP\":0.78,\"JPY\":148.75,\"CHF\":0.87,\"CAD\":1.36},\"timestamp\":1234567890}")));

        // When
        Map<String, ExchangeRateDto> rates = provider.fetchExchangeRates("USD");

        // Then
        assertThat(rates).hasSize(5);
        assertThat(rates.values()).allMatch(dto -> 
                dto != null && 
                dto.getBaseCurrency().equals("USD") && 
                dto.getProvider().equals("mock-service-2") &&
                dto.getRate() != null &&
                dto.getTimestamp() != null
        );
    }
}
