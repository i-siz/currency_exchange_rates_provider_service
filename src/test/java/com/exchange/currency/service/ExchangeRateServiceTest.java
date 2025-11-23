package com.exchange.currency.service;

import com.exchange.currency.dto.ExchangeRateDto;
import com.exchange.currency.dto.ExchangeRateRequestDto;
import com.exchange.currency.dto.ExchangeRateResponseDto;
import com.exchange.currency.entity.Currency;
import com.exchange.currency.entity.ExchangeRate;
import com.exchange.currency.exception.ExchangeRateNotFoundException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateService Unit Tests")
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private CurrencyService currencyService;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    private Currency usdCurrency;
    private Currency eurCurrency;
    private ExchangeRate exchangeRate;

    @BeforeEach
    void setUp() {
        usdCurrency = Currency.builder()
                .id(1L)
                .code("USD")
                .name("US Dollar")
                .build();

        eurCurrency = Currency.builder()
                .id(2L)
                .code("EUR")
                .name("Euro")
                .build();

        exchangeRate = ExchangeRate.builder()
                .id(1L)
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(BigDecimal.valueOf(0.92))
                .provider("test-provider")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should calculate exchange rate successfully")
    void calculateExchangeRate_ShouldReturnResult_WhenRateExists() {
        // Given
        ExchangeRateRequestDto request = ExchangeRateRequestDto.builder()
                .amount(BigDecimal.valueOf(100))
                .from("USD")
                .to("EUR")
                .build();

        when(currencyService.getCurrencyByCode("USD")).thenReturn(usdCurrency);
        when(currencyService.getCurrencyByCode("EUR")).thenReturn(eurCurrency);
        when(exchangeRateRepository.findLatestByBaseCurrencyAndTargetCurrency("USD", "EUR"))
                .thenReturn(Optional.of(exchangeRate));

        // When
        ExchangeRateResponseDto result = exchangeRateService.calculateExchangeRate(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(result.getFrom()).isEqualTo("USD");
        assertThat(result.getTo()).isEqualTo("EUR");
        assertThat(result.getRate()).isEqualByComparingTo(BigDecimal.valueOf(0.92));
        assertThat(result.getResult()).isEqualByComparingTo(BigDecimal.valueOf(92.00));

        verify(currencyService, times(1)).getCurrencyByCode("USD");
        verify(currencyService, times(1)).getCurrencyByCode("EUR");
        verify(exchangeRateRepository, times(1))
                .findLatestByBaseCurrencyAndTargetCurrency("USD", "EUR");
    }

    @Test
    @DisplayName("Should throw exception when exchange rate not found")
    void calculateExchangeRate_ShouldThrowException_WhenRateNotFound() {
        // Given
        ExchangeRateRequestDto request = ExchangeRateRequestDto.builder()
                .amount(BigDecimal.valueOf(100))
                .from("USD")
                .to("JPY")
                .build();

        when(currencyService.getCurrencyByCode("USD")).thenReturn(usdCurrency);
        when(currencyService.getCurrencyByCode("JPY")).thenReturn(
                Currency.builder().code("JPY").name("Japanese Yen").build()
        );
        when(exchangeRateRepository.findLatestByBaseCurrencyAndTargetCurrency("USD", "JPY"))
                .thenReturn(Optional.empty());
        when(exchangeRateRepository.findLatestByBaseCurrencyAndTargetCurrency("JPY", "USD"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> exchangeRateService.calculateExchangeRate(request))
                .isInstanceOf(ExchangeRateNotFoundException.class)
                .hasMessageContaining("USD")
                .hasMessageContaining("JPY");
    }

    @Test
    @DisplayName("Should save exchange rate successfully")
    void saveExchangeRate_ShouldSaveRate() {
        // Given
        ExchangeRateDto dto = ExchangeRateDto.builder()
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(BigDecimal.valueOf(0.92))
                .provider("test-provider")
                .timestamp(LocalDateTime.now())
                .build();

        when(exchangeRateRepository.save(any(ExchangeRate.class))).thenReturn(exchangeRate);

        // When
        exchangeRateService.saveExchangeRate(dto);

        // Then
        verify(exchangeRateRepository, times(1)).save(any(ExchangeRate.class));
    }

    @Test
    @DisplayName("Should get latest exchange rate")
    void getLatestRate_ShouldReturnRate_WhenExists() {
        // Given
        when(exchangeRateRepository.findLatestByBaseCurrencyAndTargetCurrency("USD", "EUR"))
                .thenReturn(Optional.of(exchangeRate));

        // When
        BigDecimal result = exchangeRateService.getLatestRate("USD", "EUR");

        // Then
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(0.92));
        verify(exchangeRateRepository, times(1))
                .findLatestByBaseCurrencyAndTargetCurrency("USD", "EUR");
    }

    @Test
    @DisplayName("Should throw exception when no rate exists")
    void getLatestRate_ShouldThrowException_WhenNotExists() {
        // Given
        when(exchangeRateRepository.findLatestByBaseCurrencyAndTargetCurrency("USD", "JPY"))
                .thenReturn(Optional.empty());
        when(exchangeRateRepository.findLatestByBaseCurrencyAndTargetCurrency("JPY", "USD"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> exchangeRateService.getLatestRate("USD", "JPY"))
                .isInstanceOf(ExchangeRateNotFoundException.class);
    }

    @Test
    @DisplayName("Should return 1.0 for same currency")
    void getLatestRate_ShouldReturnOne_WhenSameCurrency() {
        // When
        BigDecimal result = exchangeRateService.getLatestRate("USD", "USD");

        // Then
        assertThat(result).isEqualByComparingTo(BigDecimal.ONE);
        verify(exchangeRateRepository, never()).findLatestByBaseCurrencyAndTargetCurrency(any(), any());
    }
}
