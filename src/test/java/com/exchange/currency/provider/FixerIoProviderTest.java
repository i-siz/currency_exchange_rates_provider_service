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
 * Unit tests for FixerIoProvider.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FixerIoProvider Unit Tests")
class FixerIoProviderTest {

    @Mock
    private RestTemplate restTemplate;

    private FixerIoProvider provider;

    @BeforeEach
    void setUp() {
        provider = new FixerIoProvider(restTemplate);
    }

    @Test
    @DisplayName("Should return provider name")
    void shouldReturnProviderName() {
        assertThat(provider.getProviderName()).isEqualTo("fixer.io");
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
    @DisplayName("Should not be available when API key is null")
    void shouldNotBeAvailable_WhenApiKeyNull() {
        // Given
        ReflectionTestUtils.setField(provider, "enabled", true);
        ReflectionTestUtils.setField(provider, "apiKey", null);

        // Then
        assertThat(provider.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("Should not be available when API key is blank")
    void shouldNotBeAvailable_WhenApiKeyBlank() {
        // Given
        ReflectionTestUtils.setField(provider, "enabled", true);
        ReflectionTestUtils.setField(provider, "apiKey", "");

        // Then
        assertThat(provider.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("Should be available when enabled and API key configured")
    void shouldBeAvailable_WhenConfigured() {
        // Given
        ReflectionTestUtils.setField(provider, "enabled", true);
        ReflectionTestUtils.setField(provider, "apiKey", "test-key");

        // Then
        assertThat(provider.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when provider is disabled")
    void shouldThrowException_WhenDisabled() {
        // Given
        ReflectionTestUtils.setField(provider, "enabled", false);
        ReflectionTestUtils.setField(provider, "apiKey", "test-key");

        // When/Then
        assertThatThrownBy(() -> provider.fetchExchangeRates("USD"))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("not configured");

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Should throw exception when API key is not configured")
    void shouldThrowException_WhenApiKeyNotConfigured() {
        // Given
        ReflectionTestUtils.setField(provider, "enabled", true);
        ReflectionTestUtils.setField(provider, "apiKey", null);

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
                .thenThrow(new RestClientException("Connection timeout"));

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
}
