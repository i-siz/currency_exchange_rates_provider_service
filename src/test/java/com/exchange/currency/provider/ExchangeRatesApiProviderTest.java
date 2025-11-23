package com.exchange.currency.provider;

import com.exchange.currency.exception.ExternalApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExchangeRatesApiProvider.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRatesApiProvider Unit Tests")
class ExchangeRatesApiProviderTest {

    @Mock
    private RestTemplate restTemplate;

    private ExchangeRatesApiProvider provider;

    @BeforeEach
    void setUp() {
        provider = new ExchangeRatesApiProvider(restTemplate);
    }

    @Test
    @DisplayName("Should return provider name")
    void shouldReturnProviderName() {
        assertThat(provider.getProviderName()).isEqualTo("exchangeratesapi.io");
    }

    @Test
    @DisplayName("Should not be available when disabled")
    void shouldNotBeAvailable_WhenDisabled() {
        // Given
        ReflectionTestUtils.setField(provider, "enabled", false);
        ReflectionTestUtils.setField(provider, "apiKey", "test-key");

        // Then
        assertThat(provider.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("Should be available when enabled")
    void shouldBeAvailable_WhenEnabled() {
        // Given
        ReflectionTestUtils.setField(provider, "enabled", true);

        // Then
        assertThat(provider.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("Should be available when enabled without API key")
    void shouldBeAvailable_WhenEnabledWithoutApiKey() {
        // Given
        ReflectionTestUtils.setField(provider, "enabled", true);
        ReflectionTestUtils.setField(provider, "apiKey", null);

        // Then
        assertThat(provider.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when provider is disabled")
    void shouldThrowException_WhenDisabled() {
        // Given
        ReflectionTestUtils.setField(provider, "enabled", false);

        // When/Then
        assertThatThrownBy(() -> provider.fetchExchangeRates("USD"))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("not configured");

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Should throw exception when API call fails with exception")
    void shouldThrowException_WhenApiCallFails() {
        // Given
        ReflectionTestUtils.setField(provider, "enabled", true);
        ReflectionTestUtils.setField(provider, "apiKey", "test-key");

        when(restTemplate.getForObject(anyString(), any()))
                .thenThrow(new RestClientException("Network error"));

        // When/Then
        assertThatThrownBy(() -> provider.fetchExchangeRates("USD"))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Failed to fetch rates")
                .hasCauseInstanceOf(RestClientException.class);
    }

    @Test
    @DisplayName("Should throw exception when API returns null")
    void shouldThrowException_WhenApiReturnsNull() {
        // Given
        ReflectionTestUtils.setField(provider, "enabled", true);
        ReflectionTestUtils.setField(provider, "apiKey", "test-key");

        when(restTemplate.getForObject(anyString(), any()))
                .thenReturn(null);

        // When/Then
        assertThatThrownBy(() -> provider.fetchExchangeRates("USD"))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("API returned error");
    }

    @Test
    @DisplayName("Should handle API key in URL when configured")
    void shouldHandleApiKeyInUrl_WhenConfigured() {
        // Given
        ReflectionTestUtils.setField(provider, "enabled", true);
        ReflectionTestUtils.setField(provider, "apiKey", "test-key-123");

        when(restTemplate.getForObject(anyString(), any()))
                .thenReturn(null);

        // When/Then
        assertThatThrownBy(() -> provider.fetchExchangeRates("USD"))
                .isInstanceOf(ExternalApiException.class);
        
        verify(restTemplate).getForObject(anyString(), any());
    }

    @Test
    @DisplayName("Should handle empty API key")
    void shouldHandleEmptyApiKey() {
        // Given
        ReflectionTestUtils.setField(provider, "enabled", true);
        ReflectionTestUtils.setField(provider, "apiKey", "");

        when(restTemplate.getForObject(anyString(), any()))
                .thenReturn(null);

        // When/Then
        assertThatThrownBy(() -> provider.fetchExchangeRates("USD"))
                .isInstanceOf(ExternalApiException.class);
        
        verify(restTemplate).getForObject(anyString(), any());
    }

    @Test
    @DisplayName("Should throw exception for network timeout")
    void shouldThrowException_ForNetworkTimeout() {
        // Given
        ReflectionTestUtils.setField(provider, "enabled", true);

        when(restTemplate.getForObject(anyString(), any()))
                .thenThrow(new RestClientException("Read timed out"));

        // When/Then
        assertThatThrownBy(() -> provider.fetchExchangeRates("EUR"))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Failed to fetch rates");
    }

    @Test
    @DisplayName("Should throw exception for HTTP errors")
    void shouldThrowException_ForHttpErrors() {
        // Given
        ReflectionTestUtils.setField(provider, "enabled", true);

        when(restTemplate.getForObject(anyString(), any()))
                .thenThrow(new RestClientException("404 Not Found"));

        // When/Then
        assertThatThrownBy(() -> provider.fetchExchangeRates("GBP"))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Failed to fetch rates");
    }
}
