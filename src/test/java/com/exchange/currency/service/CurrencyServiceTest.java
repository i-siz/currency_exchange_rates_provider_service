package com.exchange.currency.service;

import com.exchange.currency.dto.CurrencyDto;
import com.exchange.currency.entity.Currency;
import com.exchange.currency.exception.CurrencyNotFoundException;
import com.exchange.currency.exception.InvalidCurrencyException;
import com.exchange.currency.repository.CurrencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CurrencyService Unit Tests")
class CurrencyServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @InjectMocks
    private CurrencyService currencyService;

    private Currency usdCurrency;
    private Currency eurCurrency;

    @BeforeEach
    void setUp() {
        usdCurrency = Currency.builder()
                .id(1L)
                .code("USD")
                .name("US Dollar")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        eurCurrency = Currency.builder()
                .id(2L)
                .code("EUR")
                .name("Euro")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should return all currencies")
    void getAllCurrencies_ShouldReturnAllCurrencies() {
        // Given
        when(currencyRepository.findAll()).thenReturn(Arrays.asList(usdCurrency, eurCurrency));

        // When
        List<CurrencyDto> result = currencyService.getAllCurrencies();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCode()).isEqualTo("USD");
        assertThat(result.get(0).getName()).isEqualTo("US Dollar");
        assertThat(result.get(1).getCode()).isEqualTo("EUR");
        assertThat(result.get(1).getName()).isEqualTo("Euro");
        verify(currencyRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no currencies exist")
    void getAllCurrencies_ShouldReturnEmptyList_WhenNoCurrencies() {
        // Given
        when(currencyRepository.findAll()).thenReturn(List.of());

        // When
        List<CurrencyDto> result = currencyService.getAllCurrencies();

        // Then
        assertThat(result).isEmpty();
        verify(currencyRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should add valid currency successfully")
    void addCurrency_ShouldAddCurrency_WhenValidCode() {
        // Given
        String code = "GBP";
        when(currencyRepository.existsByCode(code)).thenReturn(false);
        when(currencyRepository.save(any(Currency.class))).thenAnswer(invocation -> {
            Currency currency = invocation.getArgument(0);
            currency.setId(3L);
            return currency;
        });

        // When
        CurrencyDto result = currencyService.addCurrency(code);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(code);
        assertThat(result.getName()).isEqualTo("British Pound");

        ArgumentCaptor<Currency> captor = ArgumentCaptor.forClass(Currency.class);
        verify(currencyRepository).save(captor.capture());
        Currency savedCurrency = captor.getValue();
        assertThat(savedCurrency.getCode()).isEqualTo(code);
        assertThat(savedCurrency.getName()).isEqualTo("British Pound");
    }

    @Test
    @DisplayName("Should throw exception when adding null currency code")
    void addCurrency_ShouldThrowException_WhenCodeIsNull() {
        // When & Then
        assertThatThrownBy(() -> currencyService.addCurrency(null))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining("must be 3 uppercase letters");

        verify(currencyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when adding invalid format currency code")
    void addCurrency_ShouldThrowException_WhenInvalidFormat() {
        // When & Then
        assertThatThrownBy(() -> currencyService.addCurrency("us"))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining("must be 3 uppercase letters");

        assertThatThrownBy(() -> currencyService.addCurrency("USDD"))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining("must be 3 uppercase letters");

        assertThatThrownBy(() -> currencyService.addCurrency("usd"))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining("must be 3 uppercase letters");

        verify(currencyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when adding existing currency")
    void addCurrency_ShouldThrowException_WhenCurrencyExists() {
        // Given
        when(currencyRepository.existsByCode("USD")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> currencyService.addCurrency("USD"))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining("already exists");

        verify(currencyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when adding non-ISO currency code")
    void addCurrency_ShouldThrowException_WhenNotIsoCurrency() {
        // Given
        String invalidCode = "ZZZ";
        when(currencyRepository.existsByCode(invalidCode)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> currencyService.addCurrency(invalidCode))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining("not a valid ISO 4217 currency code");

        verify(currencyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get currency by code successfully")
    void getCurrencyByCode_ShouldReturnCurrency_WhenExists() {
        // Given
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usdCurrency));

        // When
        Currency result = currencyService.getCurrencyByCode("USD");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("USD");
        assertThat(result.getName()).isEqualTo("US Dollar");
        verify(currencyRepository, times(1)).findByCode("USD");
    }

    @Test
    @DisplayName("Should throw exception when currency not found")
    void getCurrencyByCode_ShouldThrowException_WhenNotFound() {
        // Given
        when(currencyRepository.findByCode("JPY")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> currencyService.getCurrencyByCode("JPY"))
                .isInstanceOf(CurrencyNotFoundException.class)
                .hasMessageContaining("not found in system");

        verify(currencyRepository, times(1)).findByCode("JPY");
    }

    @Test
    @DisplayName("Should return true when currency exists")
    void currencyExists_ShouldReturnTrue_WhenExists() {
        // Given
        when(currencyRepository.existsByCode("USD")).thenReturn(true);

        // When
        boolean result = currencyService.currencyExists("USD");

        // Then
        assertThat(result).isTrue();
        verify(currencyRepository, times(1)).existsByCode("USD");
    }

    @Test
    @DisplayName("Should return false when currency does not exist")
    void currencyExists_ShouldReturnFalse_WhenNotExists() {
        // Given
        when(currencyRepository.existsByCode("XXX")).thenReturn(false);

        // When
        boolean result = currencyService.currencyExists("XXX");

        // Then
        assertThat(result).isFalse();
        verify(currencyRepository, times(1)).existsByCode("XXX");
    }

    @Test
    @DisplayName("Should return all ISO currency codes")
    void getAllIsoCurrencyCodes_ShouldReturnAllCodes() {
        // When
        var result = currencyService.getAllIsoCurrencyCodes();

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).contains("USD", "EUR", "GBP", "JPY");
    }
}
