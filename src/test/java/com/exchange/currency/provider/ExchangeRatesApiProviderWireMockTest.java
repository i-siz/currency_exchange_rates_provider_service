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
 * WireMock integration tests for ExchangeRatesApiProvider.
 * Tests external API interactions with mocked HTTP responses.
 */
@DisplayName("ExchangeRatesApiProvider WireMock Tests")
class ExchangeRatesApiProviderWireMockTest {

    private WireMockServer wireMockServer;
    private ExchangeRatesApiProvider provider;
    private static final String TEST_API_KEY = "test-exchangerates-key";

    @BeforeEach
    void setUp() {
        // Start WireMock server on random port
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        // Create provider with WireMock URL
        provider = new ExchangeRatesApiProvider(new RestTemplate());
        ReflectionTestUtils.setField(provider, "apiUrl", 
                "http://localhost:" + wireMockServer.port() + "/v1/latest");
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
    @DisplayName("Should successfully fetch exchange rates with API key")
    void shouldFetchExchangeRates_WithApiKey() {
        // Given
        String baseCurrency = "USD";
        stubFor(get(urlPathEqualTo("/v1/latest"))
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
                                        "JPY": 149.50,
                                        "AUD": 1.54
                                    }
                                }
                                """)));

        // When
        Map<String, ExchangeRateDto> rates = provider.fetchExchangeRates(baseCurrency);

        // Then
        assertThat(rates).isNotNull()
                .hasSize(4)
                .containsKeys("EUR", "GBP", "JPY", "AUD");

        ExchangeRateDto eurRate = rates.get("EUR");
        assertThat(eurRate).isNotNull();
        assertThat(eurRate.getBaseCurrency()).isEqualTo(baseCurrency);
        assertThat(eurRate.getTargetCurrency()).isEqualTo("EUR");
        assertThat(eurRate.getRate()).isEqualByComparingTo(new BigDecimal("0.92"));
        assertThat(eurRate.getProvider()).isEqualTo("exchangeratesapi.io");
        assertThat(eurRate.getTimestamp()).isNotNull();

        verify(exactly(1), getRequestedFor(urlPathEqualTo("/v1/latest")));
    }

    @Test
    @DisplayName("Should successfully fetch exchange rates without API key")
    void shouldFetchExchangeRates_WithoutApiKey() {
        // Given
        ReflectionTestUtils.setField(provider, "apiKey", null);
        String baseCurrency = "EUR";
        stubFor(get(urlPathEqualTo("/v1/latest"))
                .withQueryParam("base", equalTo(baseCurrency))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "success": true,
                                    "base": "EUR",
                                    "rates": {
                                        "USD": 1.09,
                                        "GBP": 0.86
                                    }
                                }
                                """)));

        // When
        Map<String, ExchangeRateDto> rates = provider.fetchExchangeRates(baseCurrency);

        // Then
        assertThat(rates).isNotNull().hasSize(2);
        assertThat(rates.get("USD").getRate()).isEqualByComparingTo(new BigDecimal("1.09"));
    }

    @Test
    @DisplayName("Should return empty map when API returns empty rates")
    void shouldReturnEmptyMap_WhenApiReturnsEmptyRates() {
        // Given
        String baseCurrency = "USD";
        stubFor(get(urlPathEqualTo("/v1/latest"))
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
        stubFor(get(urlPathEqualTo("/v1/latest"))
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
                .hasMessageContaining("exchangeratesapi.io")
                .hasMessageContaining("API returned error");
    }

    @Test
    @DisplayName("Should throw exception when API returns null response")
    void shouldThrowException_WhenApiReturnsNullResponse() {
        // Given
        String baseCurrency = "USD";
        stubFor(get(urlPathEqualTo("/v1/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("null")));

        // When & Then
        assertThatThrownBy(() -> provider.fetchExchangeRates(baseCurrency))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("exchangeratesapi.io");
    }

    @Test
    @DisplayName("Should throw exception when API returns 401 Unauthorized")
    void shouldThrowException_WhenApiReturns401() {
        // Given
        String baseCurrency = "USD";
        stubFor(get(urlPathEqualTo("/v1/latest"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withBody("Unauthorized")));

        // When & Then
        assertThatThrownBy(() -> provider.fetchExchangeRates(baseCurrency))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("exchangeratesapi.io")
                .hasMessageContaining("Failed to fetch rates");
    }

    @Test
    @DisplayName("Should throw exception when API returns 404")
    void shouldThrowException_WhenApiReturns404() {
        // Given
        String baseCurrency = "USD";
        stubFor(get(urlPathEqualTo("/v1/latest"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Not Found")));

        // When & Then
        assertThatThrownBy(() -> provider.fetchExchangeRates(baseCurrency))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("exchangeratesapi.io");
    }

    @Test
    @DisplayName("Should throw exception when API returns 429 Too Many Requests")
    void shouldThrowException_WhenApiReturns429() {
        // Given
        String baseCurrency = "USD";
        stubFor(get(urlPathEqualTo("/v1/latest"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withBody("Too Many Requests")));

        // When & Then
        assertThatThrownBy(() -> provider.fetchExchangeRates(baseCurrency))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("exchangeratesapi.io");
    }

    @Test
    @DisplayName("Should throw exception when API returns 500")
    void shouldThrowException_WhenApiReturns500() {
        // Given
        String baseCurrency = "USD";
        stubFor(get(urlPathEqualTo("/v1/latest"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // When & Then
        assertThatThrownBy(() -> provider.fetchExchangeRates(baseCurrency))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("exchangeratesapi.io");
    }

    @Test
    @DisplayName("Should throw exception when network timeout occurs")
    void shouldThrowException_WhenNetworkTimeout() {
        // Given
        String baseCurrency = "USD";
        stubFor(get(urlPathEqualTo("/v1/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(5000) // 5 second delay
                        .withBody("{}")));

        // When & Then
        assertThatThrownBy(() -> provider.fetchExchangeRates(baseCurrency))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("exchangeratesapi.io");
    }

    @Test
    @DisplayName("Should throw exception when API returns malformed JSON")
    void shouldThrowException_WhenMalformedJson() {
        // Given
        String baseCurrency = "USD";
        stubFor(get(urlPathEqualTo("/v1/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ invalid json }")));

        // When & Then
        assertThatThrownBy(() -> provider.fetchExchangeRates(baseCurrency))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("exchangeratesapi.io");
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
        verify(exactly(0), getRequestedFor(urlPathEqualTo("/v1/latest")));
    }

    @Test
    @DisplayName("Should return provider name")
    void shouldReturnProviderName() {
        // When
        String name = provider.getProviderName();

        // Then
        assertThat(name).isEqualTo("exchangeratesapi.io");
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
        String baseCurrency = "GBP";
        stubFor(get(urlPathEqualTo("/v1/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "success": true,
                                    "base": "GBP",
                                    "rates": {
                                        "USD": 1.27,
                                        "EUR": 1.16,
                                        "JPY": 189.50,
                                        "CHF": 1.10,
                                        "AUD": 1.95,
                                        "CAD": 1.76,
                                        "CNY": 9.15
                                    }
                                }
                                """)));

        // When
        Map<String, ExchangeRateDto> rates = provider.fetchExchangeRates(baseCurrency);

        // Then
        assertThat(rates).hasSize(7);
        assertThat(rates.keySet()).containsExactlyInAnyOrder(
                "USD", "EUR", "JPY", "CHF", "AUD", "CAD", "CNY");
        
        rates.values().forEach(dto -> {
            assertThat(dto.getBaseCurrency()).isEqualTo(baseCurrency);
            assertThat(dto.getProvider()).isEqualTo("exchangeratesapi.io");
            assertThat(dto.getRate()).isNotNull().isPositive();
        });
    }

    @Test
    @DisplayName("Should handle large decimal values correctly")
    void shouldHandleLargeDecimalValues() {
        // Given
        String baseCurrency = "USD";
        stubFor(get(urlPathEqualTo("/v1/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "success": true,
                                    "base": "USD",
                                    "rates": {
                                        "JPY": 149.567890,
                                        "KRW": 1305.4567,
                                        "IDR": 15678.9012
                                    }
                                }
                                """)));

        // When
        Map<String, ExchangeRateDto> rates = provider.fetchExchangeRates(baseCurrency);

        // Then
        assertThat(rates).hasSize(3);
        assertThat(rates.get("JPY").getRate()).isEqualByComparingTo(new BigDecimal("149.567890"));
        assertThat(rates.get("KRW").getRate()).isEqualByComparingTo(new BigDecimal("1305.4567"));
        assertThat(rates.get("IDR").getRate()).isEqualByComparingTo(new BigDecimal("15678.9012"));
    }
}
