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

import java.math.BigDecimal;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * WireMock integration tests for FixerIoProvider.
 * Tests external API interactions with mocked HTTP responses.
 */
@DisplayName("FixerIoProvider WireMock Tests")
class FixerIoProviderWireMockTest {

    private WireMockServer wireMockServer;
    private FixerIoProvider provider;
    private static final String TEST_API_KEY = "test-api-key-12345";

    @BeforeEach
    void setUp() {
        // Start WireMock server on random port
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        // Create provider with WireMock URL
        provider = new FixerIoProvider(new RestTemplate());
        ReflectionTestUtils.setField(provider, "apiUrl", 
                "http://localhost:" + wireMockServer.port() + "/api/latest");
        ReflectionTestUtils.setField(provider, "apiKey", TEST_API_KEY);
        ReflectionTestUtils.setField(provider, "enabled", true);
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @Test
    @DisplayName("Should successfully fetch exchange rates with valid response")
    void shouldFetchExchangeRates_WhenApiReturnsValidResponse() {
        // Given
        String baseCurrency = "USD";
        stubFor(get(urlPathEqualTo("/api/latest"))
                .withQueryParam("access_key", equalTo(TEST_API_KEY))
                .withQueryParam("base", equalTo(baseCurrency))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "success": true,
                                    "base": "USD",
                                    "date": "2025-11-23",
                                    "rates": {
                                        "EUR": 0.92,
                                        "GBP": 0.79,
                                        "JPY": 149.50
                                    }
                                }
                                """)));

        // When
        Map<String, ExchangeRateDto> rates = provider.fetchExchangeRates(baseCurrency);

        // Then
        assertThat(rates).isNotNull()
                .hasSize(3)
                .containsKeys("EUR", "GBP", "JPY");

        ExchangeRateDto eurRate = rates.get("EUR");
        assertThat(eurRate).isNotNull();
        assertThat(eurRate.getBaseCurrency()).isEqualTo(baseCurrency);
        assertThat(eurRate.getTargetCurrency()).isEqualTo("EUR");
        assertThat(eurRate.getRate()).isEqualByComparingTo(new BigDecimal("0.92"));
        assertThat(eurRate.getProvider()).isEqualTo("fixer.io");
        assertThat(eurRate.getTimestamp()).isNotNull();

        verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/latest")));
    }

    @Test
    @DisplayName("Should return empty map when API returns empty rates")
    void shouldReturnEmptyMap_WhenApiReturnsEmptyRates() {
        // Given
        String baseCurrency = "USD";
        stubFor(get(urlPathEqualTo("/api/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "success": true,
                                    "base": "USD",
                                    "rates": {}
                                }
                                """)));

        // When
        Map<String, ExchangeRateDto> rates = provider.fetchExchangeRates(baseCurrency);

        // Then
        assertThat(rates).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should throw exception when API returns error response")
    void shouldThrowException_WhenApiReturnsError() {
        // Given
        String baseCurrency = "USD";
        stubFor(get(urlPathEqualTo("/api/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "success": false,
                                    "error": {
                                        "code": 101,
                                        "type": "invalid_access_key",
                                        "info": "You have not supplied a valid API Access Key."
                                    }
                                }
                                """)));

        // When & Then
        assertThatThrownBy(() -> provider.fetchExchangeRates(baseCurrency))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("fixer.io")
                .hasMessageContaining("API returned error");
    }

    @Test
    @DisplayName("Should throw exception when API returns null response")
    void shouldThrowException_WhenApiReturnsNullResponse() {
        // Given
        String baseCurrency = "USD";
        stubFor(get(urlPathEqualTo("/api/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("null")));

        // When & Then
        assertThatThrownBy(() -> provider.fetchExchangeRates(baseCurrency))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("fixer.io");
    }

    @Test
    @DisplayName("Should throw exception when API returns 404")
    void shouldThrowException_WhenApiReturns404() {
        // Given
        String baseCurrency = "USD";
        stubFor(get(urlPathEqualTo("/api/latest"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Not Found")));

        // When & Then
        assertThatThrownBy(() -> provider.fetchExchangeRates(baseCurrency))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("fixer.io")
                .hasMessageContaining("Failed to fetch rates");
    }

    @Test
    @DisplayName("Should throw exception when API returns 500")
    void shouldThrowException_WhenApiReturns500() {
        // Given
        String baseCurrency = "USD";
        stubFor(get(urlPathEqualTo("/api/latest"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // When & Then
        assertThatThrownBy(() -> provider.fetchExchangeRates(baseCurrency))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("fixer.io");
    }

    @Test
    @DisplayName("Should throw exception when network timeout occurs")
    void shouldThrowException_WhenNetworkTimeout() {
        // Given
        String baseCurrency = "USD";
        stubFor(get(urlPathEqualTo("/api/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(5000) // 5 second delay
                        .withBody("{}")));

        // When & Then
        assertThatThrownBy(() -> provider.fetchExchangeRates(baseCurrency))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("fixer.io");
    }

    @Test
    @DisplayName("Should throw exception when API returns malformed JSON")
    void shouldThrowException_WhenMalformedJson() {
        // Given
        String baseCurrency = "USD";
        stubFor(get(urlPathEqualTo("/api/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ invalid json }")));

        // When & Then
        assertThatThrownBy(() -> provider.fetchExchangeRates(baseCurrency))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("fixer.io");
    }

    @Test
    @DisplayName("Should throw exception when provider is disabled")
    void shouldThrowException_WhenProviderDisabled() {
        // Given
        ReflectionTestUtils.setField(provider, "enabled", false);
        String baseCurrency = "USD";

        // When & Then
        assertThatThrownBy(() -> provider.fetchExchangeRates(baseCurrency))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Provider is not configured");

        // Verify no API call was made
        verify(exactly(0), getRequestedFor(urlPathEqualTo("/api/latest")));
    }

    @Test
    @DisplayName("Should throw exception when API key is null")
    void shouldThrowException_WhenApiKeyIsNull() {
        // Given
        ReflectionTestUtils.setField(provider, "apiKey", null);
        String baseCurrency = "USD";

        // When & Then
        assertThatThrownBy(() -> provider.fetchExchangeRates(baseCurrency))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Provider is not configured");

        verify(exactly(0), getRequestedFor(urlPathEqualTo("/api/latest")));
    }

    @Test
    @DisplayName("Should return provider name")
    void shouldReturnProviderName() {
        // When
        String name = provider.getProviderName();

        // Then
        assertThat(name).isEqualTo("fixer.io");
    }

    @Test
    @DisplayName("Should return true when provider is available")
    void shouldReturnTrue_WhenProviderAvailable() {
        // When
        boolean available = provider.isAvailable();

        // Then
        assertThat(available).isTrue();
    }

    @Test
    @DisplayName("Should return false when provider is disabled")
    void shouldReturnFalse_WhenProviderDisabled() {
        // Given
        ReflectionTestUtils.setField(provider, "enabled", false);

        // When
        boolean available = provider.isAvailable();

        // Then
        assertThat(available).isFalse();
    }

    @Test
    @DisplayName("Should handle multiple currencies in response")
    void shouldHandleMultipleCurrencies() {
        // Given
        String baseCurrency = "EUR";
        stubFor(get(urlPathEqualTo("/api/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "success": true,
                                    "base": "EUR",
                                    "rates": {
                                        "USD": 1.09,
                                        "GBP": 0.86,
                                        "JPY": 162.50,
                                        "CHF": 0.95,
                                        "AUD": 1.66,
                                        "CAD": 1.47
                                    }
                                }
                                """)));

        // When
        Map<String, ExchangeRateDto> rates = provider.fetchExchangeRates(baseCurrency);

        // Then
        assertThat(rates).hasSize(6);
        assertThat(rates.keySet()).containsExactlyInAnyOrder(
                "USD", "GBP", "JPY", "CHF", "AUD", "CAD");
        
        rates.values().forEach(dto -> {
            assertThat(dto.getBaseCurrency()).isEqualTo(baseCurrency);
            assertThat(dto.getProvider()).isEqualTo("fixer.io");
            assertThat(dto.getRate()).isNotNull().isPositive();
        });
    }
}
