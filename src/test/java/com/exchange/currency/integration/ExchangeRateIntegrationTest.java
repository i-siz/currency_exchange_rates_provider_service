package com.exchange.currency.integration;

import com.exchange.currency.dto.ExchangeRateDto;
import com.exchange.currency.dto.ExchangeRateRequestDto;
import com.exchange.currency.dto.ExchangeRateResponseDto;
import com.exchange.currency.entity.ExchangeRate;
import com.exchange.currency.exception.ExchangeRateNotFoundException;
import com.exchange.currency.repository.CurrencyRepository;
import com.exchange.currency.repository.ExchangeRateRepository;
import com.exchange.currency.service.CurrencyService;
import com.exchange.currency.service.ExchangeRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for Exchange Rate functionality using TestContainers.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Exchange Rate Integration Tests")
class ExchangeRateIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.liquibase.enabled", () -> "true");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // Clean up database and cache before each test
        exchangeRateRepository.deleteAll();
        currencyRepository.deleteAll();
        if (cacheManager.getCache("exchangeRates") != null) {
            cacheManager.getCache("exchangeRates").clear();
        }

        // Add test currencies
        currencyService.addCurrency("USD");
        currencyService.addCurrency("EUR");
        currencyService.addCurrency("GBP");
    }

    @Test
    @DisplayName("Should save and retrieve exchange rate from database")
    void shouldSaveAndRetrieveExchangeRate() {
        // Given
        ExchangeRateDto dto = ExchangeRateDto.builder()
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(BigDecimal.valueOf(0.92))
                .provider("test-provider")
                .timestamp(LocalDateTime.now())
                .build();

        // When
        exchangeRateService.saveExchangeRate(dto);

        // Then
        List<ExchangeRate> rates = exchangeRateRepository.findAll();
        assertThat(rates).hasSize(1);
        assertThat(rates.get(0).getBaseCurrency()).isEqualTo("USD");
        assertThat(rates.get(0).getTargetCurrency()).isEqualTo("EUR");
        assertThat(rates.get(0).getRate()).isEqualByComparingTo(BigDecimal.valueOf(0.92));
    }

    @Test
    @DisplayName("Should calculate exchange rate using database data")
    void shouldCalculateExchangeRateUsingDatabase() {
        // Given
        ExchangeRateDto dto = ExchangeRateDto.builder()
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(BigDecimal.valueOf(0.92))
                .provider("test-provider")
                .timestamp(LocalDateTime.now())
                .build();
        exchangeRateService.saveExchangeRate(dto);

        ExchangeRateRequestDto request = ExchangeRateRequestDto.builder()
                .amount(BigDecimal.valueOf(100))
                .from("USD")
                .to("EUR")
                .build();

        // When
        ExchangeRateResponseDto response = exchangeRateService.calculateExchangeRate(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(response.getFrom()).isEqualTo("USD");
        assertThat(response.getTo()).isEqualTo("EUR");
        assertThat(response.getRate()).isEqualByComparingTo(BigDecimal.valueOf(0.92));
        assertThat(response.getResult()).isEqualByComparingTo(BigDecimal.valueOf(92.00));
    }

    @Test
    @DisplayName("Should cache exchange rates in Redis")
    void shouldCacheExchangeRatesInRedis() {
        // Given
        ExchangeRateDto dto = ExchangeRateDto.builder()
                .baseCurrency("USD")
                .targetCurrency("GBP")
                .rate(BigDecimal.valueOf(0.79))
                .provider("test-provider")
                .timestamp(LocalDateTime.now())
                .build();
        exchangeRateService.saveExchangeRate(dto);

        // When - first call hits database
        BigDecimal rate1 = exchangeRateService.getLatestRate("USD", "GBP");

        // Delete from database to verify cache is used
        exchangeRateRepository.deleteAll();

        // When - second call should use cache
        BigDecimal rate2 = exchangeRateService.getLatestRate("USD", "GBP");

        // Then - both should return the same rate (from cache on second call)
        assertThat(rate1).isEqualByComparingTo(BigDecimal.valueOf(0.79));
        assertThat(rate2).isEqualByComparingTo(BigDecimal.valueOf(0.79));
    }

    @Test
    @DisplayName("Should evict cache when saving new exchange rate")
    void shouldEvictCacheWhenSavingNewRate() {
        // Given
        ExchangeRateDto dto1 = ExchangeRateDto.builder()
                .baseCurrency("EUR")
                .targetCurrency("USD")
                .rate(BigDecimal.valueOf(1.08))
                .provider("provider1")
                .timestamp(LocalDateTime.now())
                .build();
        exchangeRateService.saveExchangeRate(dto1);

        // Get rate to populate cache
        BigDecimal cachedRate = exchangeRateService.getLatestRate("EUR", "USD");
        assertThat(cachedRate).isEqualByComparingTo(BigDecimal.valueOf(1.08));

        // When - save new rate (should evict cache)
        ExchangeRateDto dto2 = ExchangeRateDto.builder()
                .baseCurrency("EUR")
                .targetCurrency("USD")
                .rate(BigDecimal.valueOf(1.10))
                .provider("provider2")
                .timestamp(LocalDateTime.now().plusMinutes(5))
                .build();
        exchangeRateService.saveExchangeRate(dto2);

        // Then - should get new rate from database (cache evicted)
        BigDecimal newRate = exchangeRateService.getLatestRate("EUR", "USD");
        assertThat(newRate).isEqualByComparingTo(BigDecimal.valueOf(1.10));
    }

    @Test
    @DisplayName("Should throw exception when exchange rate not found")
    void shouldThrowExceptionWhenRateNotFound() {
        // Given
        ExchangeRateRequestDto request = ExchangeRateRequestDto.builder()
                .amount(BigDecimal.valueOf(100))
                .from("USD")
                .to("GBP")
                .build();

        // When & Then
        assertThatThrownBy(() -> exchangeRateService.calculateExchangeRate(request))
                .isInstanceOf(ExchangeRateNotFoundException.class)
                .hasMessageContaining("USD")
                .hasMessageContaining("GBP");
    }

    @Test
    @DisplayName("Should handle same currency conversion")
    void shouldHandleSameCurrencyConversion() {
        // Given
        ExchangeRateRequestDto request = ExchangeRateRequestDto.builder()
                .amount(BigDecimal.valueOf(100))
                .from("USD")
                .to("USD")
                .build();

        // When
        ExchangeRateResponseDto response = exchangeRateService.calculateExchangeRate(request);

        // Then
        assertThat(response.getRate()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(response.getResult()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
    }

    @Test
    @DisplayName("Should use inverse rate when direct rate not available")
    void shouldUseInverseRateWhenDirectRateNotAvailable() {
        // Given - only EUR to USD rate exists
        ExchangeRateDto dto = ExchangeRateDto.builder()
                .baseCurrency("EUR")
                .targetCurrency("USD")
                .rate(BigDecimal.valueOf(1.10))
                .provider("test-provider")
                .timestamp(LocalDateTime.now())
                .build();
        exchangeRateService.saveExchangeRate(dto);

        // When - request USD to EUR (inverse)
        BigDecimal rate = exchangeRateService.getLatestRate("USD", "EUR");

        // Then - should calculate inverse (1 / 1.10 ≈ 0.909091)
        assertThat(rate).isEqualByComparingTo(BigDecimal.valueOf(0.909091));
    }
}
