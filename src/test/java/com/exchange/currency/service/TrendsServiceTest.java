package com.exchange.currency.service;

import com.exchange.currency.dto.TrendRequestDto;
import com.exchange.currency.dto.TrendResponseDto;
import com.exchange.currency.entity.Currency;
import com.exchange.currency.entity.ExchangeRate;
import com.exchange.currency.exception.CurrencyNotFoundException;
import com.exchange.currency.exception.ExchangeRateNotFoundException;
import com.exchange.currency.exception.InvalidPeriodFormatException;
import com.exchange.currency.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TrendsService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TrendsService Unit Tests")
class TrendsServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private CurrencyService currencyService;

    @InjectMocks
    private TrendsService trendsService;

    private Currency usd;
    private Currency eur;
    private TrendRequestDto trendRequest;

    @BeforeEach
    void setUp() {
        usd = Currency.builder()
                .id(1L)
                .code("USD")
                .name("US Dollar")
                .build();

        eur = Currency.builder()
                .id(2L)
                .code("EUR")
                .name("Euro")
                .build();

        trendRequest = new TrendRequestDto();
        trendRequest.setFrom("USD");
        trendRequest.setTo("EUR");
        trendRequest.setPeriod("12H");
    }

    @Test
    @DisplayName("Should calculate trends successfully for valid period")
    void shouldCalculateTrends_Success() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earlier = now.minusHours(12);

        ExchangeRate oldRate = ExchangeRate.builder()
                .id(1L)
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("0.90"))
                .timestamp(earlier)
                .provider("test")
                .build();

        ExchangeRate newRate = ExchangeRate.builder()
                .id(2L)
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("0.95"))
                .timestamp(now)
                .provider("test")
                .build();

        List<ExchangeRate> rates = Arrays.asList(oldRate, newRate);

        when(currencyService.getCurrencyByCode("USD")).thenReturn(usd);
        when(currencyService.getCurrencyByCode("EUR")).thenReturn(eur);
        when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrencyAndTimestampBetween(
                eq("USD"), eq("EUR"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(rates);

        // When
        TrendResponseDto response = trendsService.calculateTrends(trendRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getFrom()).isEqualTo("USD");
        assertThat(response.getTo()).isEqualTo("EUR");
        assertThat(response.getPeriod()).isEqualTo("12H");
        assertThat(response.getStartRate()).isEqualByComparingTo("0.90");
        assertThat(response.getEndRate()).isEqualByComparingTo("0.95");
        assertThat(response.getChange()).isEqualByComparingTo("0.05");
        assertThat(response.getChangePercent()).isEqualByComparingTo("5.56");
        assertThat(response.getDataPoints()).hasSize(2);
        // Verify each dataPoint is non-null (kills NullReturns mutation in lambda at line 85)
        assertThat(response.getDataPoints()).allMatch(point -> point != null && point.getRate() != null && point.getTimestamp() != null);

        verify(currencyService).getCurrencyByCode("USD");
        verify(currencyService).getCurrencyByCode("EUR");
        verify(exchangeRateRepository).findByBaseCurrencyAndTargetCurrencyAndTimestampBetween(
                eq("USD"), eq("EUR"), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should calculate trends for hours period")
    void shouldCalculateTrends_HoursPeriod() {
        // Given
        trendRequest.setPeriod("24H");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusHours(24);

        List<ExchangeRate> rates = createRates(start, now, 5);

        when(currencyService.getCurrencyByCode("USD")).thenReturn(usd);
        when(currencyService.getCurrencyByCode("EUR")).thenReturn(eur);
        when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrencyAndTimestampBetween(
                eq("USD"), eq("EUR"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(rates);

        // When
        TrendResponseDto response = trendsService.calculateTrends(trendRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPeriod()).isEqualTo("24H");
        assertThat(response.getDataPoints()).hasSize(5);
    }

    @Test
    @DisplayName("Should calculate trends for days period")
    void shouldCalculateTrends_DaysPeriod() {
        // Given
        trendRequest.setPeriod("7D");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(7);

        List<ExchangeRate> rates = createRates(start, now, 7);

        when(currencyService.getCurrencyByCode("USD")).thenReturn(usd);
        when(currencyService.getCurrencyByCode("EUR")).thenReturn(eur);
        when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrencyAndTimestampBetween(
                eq("USD"), eq("EUR"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(rates);

        // When
        TrendResponseDto response = trendsService.calculateTrends(trendRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPeriod()).isEqualTo("7D");
    }

    @Test
    @DisplayName("Should calculate trends for months period")
    void shouldCalculateTrends_MonthsPeriod() {
        // Given
        trendRequest.setPeriod("3M");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusMonths(3);

        List<ExchangeRate> rates = createRates(start, now, 10);

        when(currencyService.getCurrencyByCode("USD")).thenReturn(usd);
        when(currencyService.getCurrencyByCode("EUR")).thenReturn(eur);
        when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrencyAndTimestampBetween(
                eq("USD"), eq("EUR"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(rates);

        // When
        TrendResponseDto response = trendsService.calculateTrends(trendRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPeriod()).isEqualTo("3M");
    }

    @Test
    @DisplayName("Should calculate trends for years period")
    void shouldCalculateTrends_YearsPeriod() {
        // Given
        trendRequest.setPeriod("1Y");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusYears(1);

        List<ExchangeRate> rates = createRates(start, now, 12);

        when(currencyService.getCurrencyByCode("USD")).thenReturn(usd);
        when(currencyService.getCurrencyByCode("EUR")).thenReturn(eur);
        when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrencyAndTimestampBetween(
                eq("USD"), eq("EUR"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(rates);

        // When
        TrendResponseDto response = trendsService.calculateTrends(trendRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPeriod()).isEqualTo("1Y");
    }

    @Test
    @DisplayName("Should calculate negative trends correctly")
    void shouldCalculateTrends_NegativeChange() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earlier = now.minusHours(12);

        ExchangeRate oldRate = ExchangeRate.builder()
                .id(1L)
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("1.00"))
                .timestamp(earlier)
                .provider("test")
                .build();

        ExchangeRate newRate = ExchangeRate.builder()
                .id(2L)
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("0.90"))
                .timestamp(now)
                .provider("test")
                .build();

        List<ExchangeRate> rates = Arrays.asList(oldRate, newRate);

        when(currencyService.getCurrencyByCode("USD")).thenReturn(usd);
        when(currencyService.getCurrencyByCode("EUR")).thenReturn(eur);
        when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrencyAndTimestampBetween(
                eq("USD"), eq("EUR"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(rates);

        // When
        TrendResponseDto response = trendsService.calculateTrends(trendRequest);

        // Then
        assertThat(response.getChange()).isNegative();
        assertThat(response.getChangePercent()).isNegative();
        assertThat(response.getChange()).isEqualByComparingTo("-0.10");
        assertThat(response.getChangePercent()).isEqualByComparingTo("-10.00");
    }

    @Test
    @DisplayName("Should throw exception for invalid period format")
    void shouldThrowException_InvalidPeriodFormat() {
        // Given
        trendRequest.setPeriod("invalid");

        when(currencyService.getCurrencyByCode("USD")).thenReturn(usd);
        when(currencyService.getCurrencyByCode("EUR")).thenReturn(eur);

        // When/Then
        assertThatThrownBy(() -> trendsService.calculateTrends(trendRequest))
                .isInstanceOf(InvalidPeriodFormatException.class)
                .hasMessageContaining("invalid")
                .hasMessageContaining("format: number followed by H (hours), D (days), M (months), or Y (years)");

        verify(currencyService).getCurrencyByCode("USD");
        verify(currencyService).getCurrencyByCode("EUR");
        verifyNoInteractions(exchangeRateRepository);
    }

    @Test
    @DisplayName("Should throw exception for period with no unit")
    void shouldThrowException_PeriodNoUnit() {
        // Given
        trendRequest.setPeriod("12");

        when(currencyService.getCurrencyByCode("USD")).thenReturn(usd);
        when(currencyService.getCurrencyByCode("EUR")).thenReturn(eur);

        // When/Then
        assertThatThrownBy(() -> trendsService.calculateTrends(trendRequest))
                .isInstanceOf(InvalidPeriodFormatException.class);
    }

    @Test
    @DisplayName("Should throw exception for period with invalid unit")
    void shouldThrowException_InvalidUnit() {
        // Given
        trendRequest.setPeriod("12X");

        when(currencyService.getCurrencyByCode("USD")).thenReturn(usd);
        when(currencyService.getCurrencyByCode("EUR")).thenReturn(eur);

        // When/Then
        assertThatThrownBy(() -> trendsService.calculateTrends(trendRequest))
                .isInstanceOf(InvalidPeriodFormatException.class);
    }

    @Test
    @DisplayName("Should throw exception when no rates found")
    void shouldThrowException_NoRatesFound() {
        // Given
        when(currencyService.getCurrencyByCode("USD")).thenReturn(usd);
        when(currencyService.getCurrencyByCode("EUR")).thenReturn(eur);
        when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrencyAndTimestampBetween(
                eq("USD"), eq("EUR"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When/Then
        assertThatThrownBy(() -> trendsService.calculateTrends(trendRequest))
                .isInstanceOf(ExchangeRateNotFoundException.class)
                .hasMessageContaining("No exchange rate data found for USD to EUR");

        verify(exchangeRateRepository).findByBaseCurrencyAndTargetCurrencyAndTimestampBetween(
                eq("USD"), eq("EUR"), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should throw exception when source currency not found")
    void shouldThrowException_SourceCurrencyNotFound() {
        // Given
        when(currencyService.getCurrencyByCode("USD"))
                .thenThrow(new CurrencyNotFoundException("USD", "does not exist"));

        // When/Then
        assertThatThrownBy(() -> trendsService.calculateTrends(trendRequest))
                .isInstanceOf(CurrencyNotFoundException.class)
                .hasMessageContaining("USD");

        verify(currencyService).getCurrencyByCode("USD");
        verifyNoMoreInteractions(currencyService);
        verifyNoInteractions(exchangeRateRepository);
    }

    @Test
    @DisplayName("Should throw exception when target currency not found")
    void shouldThrowException_TargetCurrencyNotFound() {
        // Given
        when(currencyService.getCurrencyByCode("USD")).thenReturn(usd);
        when(currencyService.getCurrencyByCode("EUR"))
                .thenThrow(new CurrencyNotFoundException("EUR", "does not exist"));

        // When/Then
        assertThatThrownBy(() -> trendsService.calculateTrends(trendRequest))
                .isInstanceOf(CurrencyNotFoundException.class)
                .hasMessageContaining("EUR");

        verify(currencyService).getCurrencyByCode("USD");
        verify(currencyService).getCurrencyByCode("EUR");
        verifyNoInteractions(exchangeRateRepository);
    }

    @Test
    @DisplayName("Should handle single rate data point")
    void shouldCalculateTrends_SingleDataPoint() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        ExchangeRate rate = ExchangeRate.builder()
                .id(1L)
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("0.92"))
                .timestamp(now)
                .provider("test")
                .build();

        List<ExchangeRate> rates = Collections.singletonList(rate);

        when(currencyService.getCurrencyByCode("USD")).thenReturn(usd);
        when(currencyService.getCurrencyByCode("EUR")).thenReturn(eur);
        when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrencyAndTimestampBetween(
                eq("USD"), eq("EUR"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(rates);

        // When
        TrendResponseDto response = trendsService.calculateTrends(trendRequest);

        // Then
        assertThat(response.getStartRate()).isEqualByComparingTo(response.getEndRate());
        assertThat(response.getChange()).isEqualByComparingTo("0.00");
        assertThat(response.getChangePercent()).isEqualByComparingTo("0.00");
        assertThat(response.getDataPoints()).hasSize(1);
    }

    @Test
    @DisplayName("Should handle very small rate changes")
    void shouldCalculateTrends_SmallChanges() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earlier = now.minusHours(1);

        ExchangeRate oldRate = ExchangeRate.builder()
                .id(1L)
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("0.920000"))
                .timestamp(earlier)
                .provider("test")
                .build();

        ExchangeRate newRate = ExchangeRate.builder()
                .id(2L)
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("0.920500"))  // 0.05% change - detectable
                .timestamp(now)
                .provider("test")
                .build();

        List<ExchangeRate> rates = Arrays.asList(oldRate, newRate);

        when(currencyService.getCurrencyByCode("USD")).thenReturn(usd);
        when(currencyService.getCurrencyByCode("EUR")).thenReturn(eur);
        when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrencyAndTimestampBetween(
                eq("USD"), eq("EUR"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(rates);

        // When
        TrendResponseDto response = trendsService.calculateTrends(trendRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getChange()).isPositive();
        assertThat(response.getChangePercent()).isPositive();
    }

    @Test
    @DisplayName("Should handle large period numbers")
    void shouldCalculateTrends_LargePeriod() {
        // Given
        trendRequest.setPeriod("365D");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(365);

        List<ExchangeRate> rates = createRates(start, now, 365);

        when(currencyService.getCurrencyByCode("USD")).thenReturn(usd);
        when(currencyService.getCurrencyByCode("EUR")).thenReturn(eur);
        when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrencyAndTimestampBetween(
                eq("USD"), eq("EUR"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(rates);

        // When
        TrendResponseDto response = trendsService.calculateTrends(trendRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPeriod()).isEqualTo("365D");
        assertThat(response.getDataPoints()).hasSize(365);
    }

    /**
     * Helper method to create a list of exchange rates.
     */
    private List<ExchangeRate> createRates(LocalDateTime start, LocalDateTime end, int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> {
                    LocalDateTime timestamp = start.plusHours(i * 24L / count);
                    BigDecimal rate = new BigDecimal("0.90").add(
                            new BigDecimal(i * 0.01 / count));
                    
                    return ExchangeRate.builder()
                            .id((long) i)
                            .baseCurrency("USD")
                            .targetCurrency("EUR")
                            .rate(rate)
                            .timestamp(timestamp)
                            .provider("test")
                            .build();
                })
                .toList();
    }
}
