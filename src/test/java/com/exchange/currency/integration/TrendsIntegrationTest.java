package com.exchange.currency.integration;

import com.exchange.currency.dto.TrendRequestDto;
import com.exchange.currency.dto.TrendResponseDto;
import com.exchange.currency.entity.ExchangeRate;
import com.exchange.currency.exception.CurrencyNotFoundException;
import com.exchange.currency.exception.ExchangeRateNotFoundException;
import com.exchange.currency.exception.InvalidPeriodFormatException;
import com.exchange.currency.repository.CurrencyRepository;
import com.exchange.currency.repository.ExchangeRateRepository;
import com.exchange.currency.service.CurrencyService;
import com.exchange.currency.service.TrendsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Integration tests for Trends functionality using TestContainers.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Trends Integration Tests")
class TrendsIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(false);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.liquibase.enabled", () -> "true");
    }

    @Autowired
    private TrendsService trendsService;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        exchangeRateRepository.deleteAll();
        currencyRepository.deleteAll();

        // Add test currencies
        currencyService.addCurrency("USD");
        currencyService.addCurrency("EUR");
        currencyService.addCurrency("GBP");
    }

    @Test
    @DisplayName("Should calculate trend for hourly period (12H)")
    void shouldCalculateTrendForHourlyPeriod() {
        // Given - create historical data for last 12 hours
        LocalDateTime now = LocalDateTime.now();
        List<ExchangeRate> historicalRates = new ArrayList<>();
        
        // Create rates every 2 hours with increasing values (7 data points: from 12h ago to latest)
        // Start from slightly more than 12 hours ago to ensure first point is within period range
        LocalDateTime startTime = now.minusHours(12).minusMinutes(5);
        for (int i = 0; i <= 6; i++) {
            ExchangeRate rate = ExchangeRate.builder()
                    .baseCurrency("USD")
                    .targetCurrency("EUR")
                    .rate(BigDecimal.valueOf(0.90 + (0.02 * i))) // rates: 0.90 to 1.02
                    .provider("test-provider")
                    .timestamp(startTime.plusHours(i * 2))
                    .build();
            historicalRates.add(rate);
        }
        exchangeRateRepository.saveAll(historicalRates);

        TrendRequestDto request = TrendRequestDto.builder()
                .from("USD")
                .to("EUR")
                .period("12H")
                .build();

        // When
        TrendResponseDto response = trendsService.calculateTrends(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getFrom()).isEqualTo("USD");
        assertThat(response.getTo()).isEqualTo("EUR");
        assertThat(response.getPeriod()).isEqualTo("12H");
        assertThat(response.getEndRate()).isGreaterThan(response.getStartRate());
        assertThat(response.getChange()).isPositive();
        assertThat(response.getChangePercent()).isPositive();
        // Expecting 6 or 7 data points depending on timing precision
        assertThat(response.getDataPoints().size()).isGreaterThanOrEqualTo(6).isLessThanOrEqualTo(7);
    }

    @Test
    @DisplayName("Should calculate trend for daily period (10D)")
    void shouldCalculateTrendForDailyPeriod() {
        // Given - create historical data for last 10 days
        LocalDateTime now = LocalDateTime.now();
        List<ExchangeRate> historicalRates = new ArrayList<>();
        
        // Create rates every day with fluctuating values (11 data points: from 10 days ago to latest)
        // Start from slightly more than 10 days ago to ensure first point is within period range
        LocalDateTime startTime = now.minusDays(10).minusHours(1);
        for (int i = 0; i <= 10; i++) {
            ExchangeRate rate = ExchangeRate.builder()
                    .baseCurrency("USD")
                    .targetCurrency("GBP")
                    .rate(BigDecimal.valueOf(0.80 + (0.005 * i)))
                    .provider("test-provider")
                    .timestamp(startTime.plusDays(i))
                    .build();
            historicalRates.add(rate);
        }
        exchangeRateRepository.saveAll(historicalRates);

        TrendRequestDto request = TrendRequestDto.builder()
                .from("USD")
                .to("GBP")
                .period("10D")
                .build();

        // When
        TrendResponseDto response = trendsService.calculateTrends(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getFrom()).isEqualTo("USD");
        assertThat(response.getTo()).isEqualTo("GBP");
        assertThat(response.getPeriod()).isEqualTo("10D");
        // Rates increase from 0.800 to 0.850 in steps
        assertThat(response.getStartRate()).isGreaterThanOrEqualTo(BigDecimal.valueOf(0.800));
        assertThat(response.getEndRate()).isLessThanOrEqualTo(BigDecimal.valueOf(0.850));
        assertThat(response.getEndRate()).isGreaterThan(response.getStartRate());
        // Expecting 10 or 11 data points depending on timing precision
        assertThat(response.getDataPoints().size()).isGreaterThanOrEqualTo(10).isLessThanOrEqualTo(11);
        assertThat(response.getStartTime()).isBefore(response.getEndTime());
    }

    @Test
    @DisplayName("Should calculate trend for monthly period (3M)")
    void shouldCalculateTrendForMonthlyPeriod() {
        // Given - create historical data for last 3 months
        LocalDateTime now = LocalDateTime.now();
        List<ExchangeRate> historicalRates = new ArrayList<>();
        
        // Create rates every 10 days
        for (int i = 90; i >= 0; i -= 10) {
            ExchangeRate rate = ExchangeRate.builder()
                    .baseCurrency("EUR")
                    .targetCurrency("USD")
                    .rate(BigDecimal.valueOf(1.10 - (0.01 * (90 - i) / 10)))
                    .provider("test-provider")
                    .timestamp(now.minusDays(i))
                    .build();
            historicalRates.add(rate);
        }
        exchangeRateRepository.saveAll(historicalRates);

        TrendRequestDto request = TrendRequestDto.builder()
                .from("EUR")
                .to("USD")
                .period("3M")
                .build();

        // When
        TrendResponseDto response = trendsService.calculateTrends(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getFrom()).isEqualTo("EUR");
        assertThat(response.getTo()).isEqualTo("USD");
        assertThat(response.getPeriod()).isEqualTo("3M");
        assertThat(response.getDataPoints()).hasSize(10);
        assertThat(response.getChange()).isNotNull();
        assertThat(response.getChangePercent()).isNotNull();
    }

    @Test
    @DisplayName("Should calculate trend for yearly period (1Y)")
    void shouldCalculateTrendForYearlyPeriod() {
        // Given - create historical data for last year
        LocalDateTime now = LocalDateTime.now();
        List<ExchangeRate> historicalRates = new ArrayList<>();
        
        // Create rates every month (13 data points: from 12 months ago to latest)
        // Start from slightly more than 12 months ago to ensure first point is within period range
        LocalDateTime startTime = now.minusMonths(12).minusDays(1);
        for (int i = 0; i <= 12; i++) {
            ExchangeRate rate = ExchangeRate.builder()
                    .baseCurrency("USD")
                    .targetCurrency("EUR")
                    .rate(BigDecimal.valueOf(0.92 + (0.01 * i)))
                    .provider("test-provider")
                    .timestamp(startTime.plusMonths(i))
                    .build();
            historicalRates.add(rate);
        }
        exchangeRateRepository.saveAll(historicalRates);

        TrendRequestDto request = TrendRequestDto.builder()
                .from("USD")
                .to("EUR")
                .period("1Y")
                .build();

        // When
        TrendResponseDto response = trendsService.calculateTrends(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getFrom()).isEqualTo("USD");
        assertThat(response.getTo()).isEqualTo("EUR");
        assertThat(response.getPeriod()).isEqualTo("1Y");
        // Rates increase from 0.92 to 1.04 in monthly steps
        assertThat(response.getStartRate()).isGreaterThanOrEqualTo(BigDecimal.valueOf(0.92));
        assertThat(response.getEndRate()).isLessThanOrEqualTo(BigDecimal.valueOf(1.04));
        assertThat(response.getEndRate()).isGreaterThan(response.getStartRate());
        // Expecting 12 or 13 data points depending on timing precision
        assertThat(response.getDataPoints().size()).isGreaterThanOrEqualTo(12).isLessThanOrEqualTo(13);
    }

    @Test
    @DisplayName("Should calculate negative trend when rate decreases")
    void shouldCalculateNegativeTrend() {
        // Given - create decreasing trend
        LocalDateTime now = LocalDateTime.now();
        List<ExchangeRate> historicalRates = new ArrayList<>();
        
        for (int i = 24; i >= 0; i -= 4) {
            ExchangeRate rate = ExchangeRate.builder()
                    .baseCurrency("USD")
                    .targetCurrency("EUR")
                    .rate(BigDecimal.valueOf(1.00 - (0.02 * (24 - i) / 4)))
                    .provider("test-provider")
                    .timestamp(now.minusHours(i))
                    .build();
            historicalRates.add(rate);
        }
        exchangeRateRepository.saveAll(historicalRates);

        TrendRequestDto request = TrendRequestDto.builder()
                .from("USD")
                .to("EUR")
                .period("24H")
                .build();

        // When
        TrendResponseDto response = trendsService.calculateTrends(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getChange()).isNegative();
        assertThat(response.getChangePercent()).isNegative();
        assertThat(response.getStartRate()).isGreaterThan(response.getEndRate());
    }

    @Test
    @DisplayName("Should throw exception when no data exists for period")
    void shouldThrowExceptionWhenNoDataExists() {
        // Given - no historical data
        TrendRequestDto request = TrendRequestDto.builder()
                .from("USD")
                .to("EUR")
                .period("12H")
                .build();

        // When & Then
        assertThatThrownBy(() -> trendsService.calculateTrends(request))
                .isInstanceOf(ExchangeRateNotFoundException.class)
                .hasMessageContaining("No exchange rate data found");
    }

    @Test
    @DisplayName("Should throw exception when currency does not exist")
    void shouldThrowExceptionWhenCurrencyNotExists() {
        // Given
        TrendRequestDto request = TrendRequestDto.builder()
                .from("USD")
                .to("XXX")  // Non-existent currency
                .period("12H")
                .build();

        // When & Then
        assertThatThrownBy(() -> trendsService.calculateTrends(request))
                .isInstanceOf(CurrencyNotFoundException.class)
                .hasMessageContaining("XXX");
    }

    @Test
    @DisplayName("Should throw exception for invalid period format")
    void shouldThrowExceptionForInvalidPeriodFormat() {
        // Given - create some data
        LocalDateTime now = LocalDateTime.now();
        ExchangeRate rate = ExchangeRate.builder()
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(BigDecimal.valueOf(0.92))
                .provider("test-provider")
                .timestamp(now)
                .build();
        exchangeRateRepository.save(rate);

        TrendRequestDto request = TrendRequestDto.builder()
                .from("USD")
                .to("EUR")
                .period("INVALID")
                .build();

        // When & Then
        assertThatThrownBy(() -> trendsService.calculateTrends(request))
                .isInstanceOf(InvalidPeriodFormatException.class);
    }

    @Test
    @DisplayName("Should handle single data point gracefully")
    void shouldHandleSingleDataPoint() {
        // Given - only one data point
        LocalDateTime now = LocalDateTime.now();
        ExchangeRate rate = ExchangeRate.builder()
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(BigDecimal.valueOf(0.92))
                .provider("test-provider")
                .timestamp(now.minusHours(5))
                .build();
        exchangeRateRepository.save(rate);

        TrendRequestDto request = TrendRequestDto.builder()
                .from("USD")
                .to("EUR")
                .period("12H")
                .build();

        // When
        TrendResponseDto response = trendsService.calculateTrends(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getDataPoints()).hasSize(1);
        assertThat(response.getStartRate()).isEqualByComparingTo(response.getEndRate());
        assertThat(response.getChange()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getChangePercent()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should include all data points in response")
    void shouldIncludeAllDataPointsInResponse() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<ExchangeRate> historicalRates = new ArrayList<>();
        
        for (int i = 5; i >= 0; i--) {
            ExchangeRate rate = ExchangeRate.builder()
                    .baseCurrency("USD")
                    .targetCurrency("EUR")
                    .rate(BigDecimal.valueOf(0.90 + (0.01 * (5 - i))))
                    .provider("test-provider")
                    .timestamp(now.minusDays(i))
                    .build();
            historicalRates.add(rate);
        }
        exchangeRateRepository.saveAll(historicalRates);

        TrendRequestDto request = TrendRequestDto.builder()
                .from("USD")
                .to("EUR")
                .period("10D")
                .build();

        // When
        TrendResponseDto response = trendsService.calculateTrends(request);

        // Then
        assertThat(response.getDataPoints()).hasSize(6);
        assertThat(response.getDataPoints()).allMatch(point -> 
                point.getTimestamp() != null && point.getRate() != null);
        
        // Verify data points are in chronological order
        for (int i = 1; i < response.getDataPoints().size(); i++) {
            assertThat(response.getDataPoints().get(i).getTimestamp())
                    .isAfterOrEqualTo(response.getDataPoints().get(i - 1).getTimestamp());
        }
    }

    @Test
    @DisplayName("Should handle large period numbers correctly")
    void shouldHandleLargePeriodNumbers() {
        // Given - data spanning 100 hours
        LocalDateTime now = LocalDateTime.now();
        ExchangeRate oldRate = ExchangeRate.builder()
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(BigDecimal.valueOf(0.85))
                .provider("test-provider")
                .timestamp(now.minusHours(95))
                .build();
        
        ExchangeRate recentRate = ExchangeRate.builder()
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(BigDecimal.valueOf(0.95))
                .provider("test-provider")
                .timestamp(now.minusHours(1))
                .build();
        
        exchangeRateRepository.saveAll(List.of(oldRate, recentRate));

        TrendRequestDto request = TrendRequestDto.builder()
                .from("USD")
                .to("EUR")
                .period("100H")
                .build();

        // When
        TrendResponseDto response = trendsService.calculateTrends(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPeriod()).isEqualTo("100H");
        assertThat(response.getDataPoints()).hasSize(2);
    }

    @Test
    @DisplayName("Should calculate correct percentage with high precision")
    void shouldCalculatePercentageWithHighPrecision() {
        // Given - create precise data
        LocalDateTime now = LocalDateTime.now();
        
        // Create data points well within the 7D period range to avoid boundary issues
        ExchangeRate startRate = ExchangeRate.builder()
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("0.923456"))
                .provider("test-provider")
                .timestamp(now.minusDays(6)) // 6 days ago, safely within 7D range
                .build();
        
        ExchangeRate endRate = ExchangeRate.builder()
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("0.987654"))
                .provider("test-provider")
                .timestamp(now.minusHours(1)) // 1 hour ago, safely within range
                .build();
        
        exchangeRateRepository.saveAll(List.of(startRate, endRate));

        TrendRequestDto request = TrendRequestDto.builder()
                .from("USD")
                .to("EUR")
                .period("7D")
                .build();

        // When
        TrendResponseDto response = trendsService.calculateTrends(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getChange()).isNotNull();
        assertThat(response.getChangePercent()).isNotNull();
        
        // Verify percentage has 2 decimal places
        assertThat(response.getChangePercent().scale()).isEqualTo(2);
        
        // With two different rates, there should be a non-zero change
        BigDecimal expectedStartRate = new BigDecimal("0.923456");
        BigDecimal expectedEndRate = new BigDecimal("0.987654");
        BigDecimal expectedChange = expectedEndRate.subtract(expectedStartRate);
        BigDecimal expectedPercent = expectedChange
                .divide(expectedStartRate, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        
        // Verify change percent is approximately 6.95%
        assertThat(response.getChangePercent()).isCloseTo(
                expectedPercent, within(new BigDecimal("0.05")));
    }
}
