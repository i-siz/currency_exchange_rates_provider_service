package com.exchange.currency.service;

import com.exchange.currency.dto.ExchangeRateDto;
import com.exchange.currency.provider.ExchangeRateProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ExchangeRateAggregatorService.
 * Focus on killing mutations: Increments, PrimitiveReturns, VoidMethodCall, Math, NegateConditionals, EmptyObjectReturns.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateAggregatorService Tests")
class ExchangeRateAggregatorServiceTest {

    @Mock
    private ExchangeRateProvider provider1;

    @Mock
    private ExchangeRateProvider provider2;

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private ExchangeRateAggregatorService aggregatorService;

    private List<ExchangeRateProvider> providers;

    @BeforeEach
    void setUp() {
        providers = List.of(provider1, provider2);
        aggregatorService = new ExchangeRateAggregatorService(providers, exchangeRateService);
    }

    @Test
    @DisplayName("fetchFromAllProviders should aggregate rates from multiple available providers")
    void fetchFromAllProviders_ShouldAggregateRates_WhenMultipleProvidersAvailable() {
        // Given
        String baseCurrency = "USD";
        
        Map<String, ExchangeRateDto> provider1Rates = new HashMap<>();
        provider1Rates.put("EUR", createDto("USD", "EUR", new BigDecimal("0.85"), "Provider1"));
        provider1Rates.put("GBP", createDto("USD", "GBP", new BigDecimal("0.73"), "Provider1"));

        Map<String, ExchangeRateDto> provider2Rates = new HashMap<>();
        provider2Rates.put("EUR", createDto("USD", "EUR", new BigDecimal("0.86"), "Provider2"));
        provider2Rates.put("JPY", createDto("USD", "JPY", new BigDecimal("110.5"), "Provider2"));

        when(provider1.isAvailable()).thenReturn(true);
        when(provider2.isAvailable()).thenReturn(true);
        when(provider1.fetchExchangeRates(baseCurrency)).thenReturn(provider1Rates);
        when(provider2.fetchExchangeRates(baseCurrency)).thenReturn(provider2Rates);
        when(provider1.getProviderName()).thenReturn("Provider1");
        when(provider2.getProviderName()).thenReturn("Provider2");

        // When
        Map<String, List<ExchangeRateDto>> result = aggregatorService.fetchFromAllProviders(baseCurrency);

        // Then
        assertThat(result).isNotEmpty(); // Kills EmptyObjectReturns mutation
        assertThat(result).hasSize(3);
        assertThat(result.get("EUR")).hasSize(2);
        assertThat(result.get("GBP")).hasSize(1);
        assertThat(result.get("JPY")).hasSize(1);
        
        verify(provider1).fetchExchangeRates(baseCurrency);
        verify(provider2).fetchExchangeRates(baseCurrency);
    }

    @Test
    @DisplayName("fetchFromAllProviders should skip unavailable providers")
    void fetchFromAllProviders_ShouldSkipUnavailableProviders() {
        // Given
        String baseCurrency = "USD";
        
        Map<String, ExchangeRateDto> provider1Rates = new HashMap<>();
        provider1Rates.put("EUR", createDto("USD", "EUR", new BigDecimal("0.85"), "Provider1"));

        when(provider1.isAvailable()).thenReturn(true);
        when(provider2.isAvailable()).thenReturn(false); // Provider 2 unavailable
        when(provider1.fetchExchangeRates(baseCurrency)).thenReturn(provider1Rates);
        when(provider1.getProviderName()).thenReturn("Provider1");

        // When
        Map<String, List<ExchangeRateDto>> result = aggregatorService.fetchFromAllProviders(baseCurrency);

        // Then - kills NegateConditionals mutation (isAvailable check)
        assertThat(result).hasSize(1);
        assertThat(result.get("EUR")).hasSize(1);
        
        verify(provider1).fetchExchangeRates(baseCurrency);
        verify(provider2, never()).fetchExchangeRates(anyString()); // Unavailable provider not called
    }

    @Test
    @DisplayName("fetchFromAllProviders should handle provider failures gracefully")
    void fetchFromAllProviders_ShouldContinue_WhenProviderThrowsException() {
        // Given
        String baseCurrency = "USD";
        
        Map<String, ExchangeRateDto> provider2Rates = new HashMap<>();
        provider2Rates.put("EUR", createDto("USD", "EUR", new BigDecimal("0.86"), "Provider2"));

        when(provider1.isAvailable()).thenReturn(true);
        when(provider2.isAvailable()).thenReturn(true);
        when(provider1.fetchExchangeRates(baseCurrency)).thenThrow(new RuntimeException("API Error"));
        when(provider2.fetchExchangeRates(baseCurrency)).thenReturn(provider2Rates);
        when(provider1.getProviderName()).thenReturn("Provider1");
        when(provider2.getProviderName()).thenReturn("Provider2");

        // When
        Map<String, List<ExchangeRateDto>> result = aggregatorService.fetchFromAllProviders(baseCurrency);

        // Then - kills VoidMethodCall mutation (forEach continues despite exception)
        assertThat(result).hasSize(1);
        assertThat(result.get("EUR")).hasSize(1);
        assertThat(result.get("EUR").get(0).getProvider()).isEqualTo("Provider2");
    }

    @Test
    @DisplayName("fetchAndSaveBestRates should save best rates and return correct count")
    void fetchAndSaveBestRates_ShouldSaveBestRatesAndReturnCount() {
        // Given
        String baseCurrency = "USD";
        
        Map<String, ExchangeRateDto> provider1Rates = new HashMap<>();
        provider1Rates.put("EUR", createDto("USD", "EUR", new BigDecimal("0.85"), "Provider1"));
        provider1Rates.put("GBP", createDto("USD", "GBP", new BigDecimal("0.73"), "Provider1"));

        when(provider1.isAvailable()).thenReturn(true);
        when(provider2.isAvailable()).thenReturn(false);
        when(provider1.fetchExchangeRates(baseCurrency)).thenReturn(provider1Rates);
        when(provider1.getProviderName()).thenReturn("Provider1");

        // When
        int savedCount = aggregatorService.fetchAndSaveBestRates(baseCurrency);

        // Then - kills Increments mutation (savedCount++) and PrimitiveReturns mutation (return savedCount)
        assertThat(savedCount).isEqualTo(2); // Verify correct count, not 0
        assertThat(savedCount).isPositive(); // Ensure increment worked (not negative from -1 mutation)
        
        // Kills VoidMethodCall mutation (saveExchangeRate must be called)
        verify(exchangeRateService, times(2)).saveExchangeRate(any(ExchangeRateDto.class));
    }

    @Test
    @DisplayName("fetchAndSaveBestRates should return 0 when no providers available")
    void fetchAndSaveBestRates_ShouldReturnZero_WhenNoProvidersAvailable() {
        // Given
        String baseCurrency = "USD";

        when(provider1.isAvailable()).thenReturn(false);
        when(provider2.isAvailable()).thenReturn(false);

        // When
        int savedCount = aggregatorService.fetchAndSaveBestRates(baseCurrency);

        // Then - kills PrimitiveReturns mutation (ensure return 0 is meaningful)
        assertThat(savedCount).isZero();
        
        verify(exchangeRateService, never()).saveExchangeRate(any());
    }

    @Test
    @DisplayName("fetchAndSaveBestRates should continue saving when one save fails")
    void fetchAndSaveBestRates_ShouldContinue_WhenSaveThrowsException() {
        // Given
        String baseCurrency = "USD";
        
        ExchangeRateDto eurDto = createDto("USD", "EUR", new BigDecimal("0.85"), "Provider1");
        ExchangeRateDto gbpDto = createDto("USD", "GBP", new BigDecimal("0.73"), "Provider1");
        
        Map<String, ExchangeRateDto> provider1Rates = new HashMap<>();
        provider1Rates.put("EUR", eurDto);
        provider1Rates.put("GBP", gbpDto);

        when(provider1.isAvailable()).thenReturn(true);
        when(provider2.isAvailable()).thenReturn(false);
        when(provider1.fetchExchangeRates(baseCurrency)).thenReturn(provider1Rates);
        when(provider1.getProviderName()).thenReturn("Provider1");
        
        // First save fails, second succeeds
        doThrow(new RuntimeException("DB Error")).when(exchangeRateService).saveExchangeRate(eurDto);

        // When
        int savedCount = aggregatorService.fetchAndSaveBestRates(baseCurrency);

        // Then - kills NegateConditionals mutation (bestRate.isPresent check)
        assertThat(savedCount).isEqualTo(1); // Only GBP saved successfully
        
        verify(exchangeRateService, times(2)).saveExchangeRate(any());
    }

    @Test
    @DisplayName("selectBestRate should return empty Optional for null list")
    void selectBestRate_ShouldReturnEmpty_WhenListIsNull() {
        // When - use reflection to call private method
        Optional<ExchangeRateDto> result = invokeSelectBestRate(null);

        // Then - kills NegateConditionals and EmptyObjectReturns mutations
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("selectBestRate should return empty Optional for empty list")
    void selectBestRate_ShouldReturnEmpty_WhenListIsEmpty() {
        // When
        Optional<ExchangeRateDto> result = invokeSelectBestRate(new ArrayList<>());

        // Then - kills NegateConditionals and EmptyObjectReturns mutations
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("selectBestRate should return single rate for single-element list")
    void selectBestRate_ShouldReturnSingleRate_WhenListHasOneElement() {
        // Given
        ExchangeRateDto dto = createDto("USD", "EUR", new BigDecimal("0.85"), "Provider1");
        List<ExchangeRateDto> rates = List.of(dto);

        // When
        Optional<ExchangeRateDto> result = invokeSelectBestRate(rates);

        // Then - kills NegateConditionals mutation (size == 1 check)
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(dto);
    }

    @Test
    @DisplayName("selectBestRate should return median rate for multiple rates")
    void selectBestRate_ShouldReturnMedianRate_WhenMultipleRates() {
        // Given - 5 rates to test median selection and sorting
        List<ExchangeRateDto> rates = new ArrayList<>();
        rates.add(createDto("USD", "EUR", new BigDecimal("0.90"), "Provider1"));
        rates.add(createDto("USD", "EUR", new BigDecimal("0.82"), "Provider2"));
        rates.add(createDto("USD", "EUR", new BigDecimal("0.85"), "Provider3")); // Median
        rates.add(createDto("USD", "EUR", new BigDecimal("0.88"), "Provider4"));
        rates.add(createDto("USD", "EUR", new BigDecimal("0.80"), "Provider5"));

        // When
        Optional<ExchangeRateDto> result = invokeSelectBestRate(rates);

        // Then - kills VoidMethodCall mutation (List.sort must be called)
        // and kills Math mutation (division for median calculation)
        assertThat(result).isPresent();
        assertThat(result.get().getRate()).isEqualByComparingTo(new BigDecimal("0.85"));
        assertThat(result.get().getProvider()).isEqualTo("Provider3");
    }

    @Test
    @DisplayName("selectBestRate should handle even number of rates correctly")
    void selectBestRate_ShouldReturnMedianRate_WhenEvenNumberOfRates() {
        // Given - 4 rates, median index = 4/2 = 2 (0-indexed: third element after sorting)
        List<ExchangeRateDto> rates = new ArrayList<>();
        rates.add(createDto("USD", "EUR", new BigDecimal("0.87"), "Provider1"));
        rates.add(createDto("USD", "EUR", new BigDecimal("0.83"), "Provider2"));
        rates.add(createDto("USD", "EUR", new BigDecimal("0.85"), "Provider3")); // Index 2 after sorting
        rates.add(createDto("USD", "EUR", new BigDecimal("0.81"), "Provider4"));

        // When
        Optional<ExchangeRateDto> result = invokeSelectBestRate(rates);

        // Then - kills Math mutation (size/2 division must work correctly, not multiplication)
        assertThat(result).isPresent();
        // After sorting: 0.81, 0.83, 0.85, 0.87 -> median index 2 = 0.85
        assertThat(result.get().getRate()).isEqualByComparingTo(new BigDecimal("0.85"));
    }

    @Test
    @DisplayName("selectBestRate should sort rates by comparing BigDecimal values")
    void selectBestRate_ShouldSortCorrectly() {
        // Given - unsorted rates
        List<ExchangeRateDto> rates = new ArrayList<>();
        rates.add(createDto("USD", "EUR", new BigDecimal("0.95"), "Provider1"));
        rates.add(createDto("USD", "EUR", new BigDecimal("0.75"), "Provider2"));
        rates.add(createDto("USD", "EUR", new BigDecimal("0.85"), "Provider3"));

        // When
        Optional<ExchangeRateDto> result = invokeSelectBestRate(rates);

        // Then - kills VoidMethodCall mutation (sort must execute) 
        // and PrimitiveReturns in lambda comparator
        assertThat(result).isPresent();
        assertThat(result.get().getRate()).isEqualByComparingTo(new BigDecimal("0.85"));
    }

    @Test
    @DisplayName("getAvailableProviders should return list of available provider names")
    void getAvailableProviders_ShouldReturnAvailableProviders() {
        // Given
        when(provider1.isAvailable()).thenReturn(true);
        when(provider2.isAvailable()).thenReturn(false);
        when(provider1.getProviderName()).thenReturn("Provider1");

        // When
        List<String> result = aggregatorService.getAvailableProviders();

        // Then - kills EmptyObjectReturns mutation
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly("Provider1");
    }

    @Test
    @DisplayName("getAvailableProviders should return empty list when no providers available")
    void getAvailableProviders_ShouldReturnEmptyList_WhenNoProvidersAvailable() {
        // Given
        when(provider1.isAvailable()).thenReturn(false);
        when(provider2.isAvailable()).thenReturn(false);

        // When
        List<String> result = aggregatorService.getAvailableProviders();

        // Then - ensure method handles empty case correctly
        assertThat(result).isEmpty();
    }

    // Helper method to invoke private selectBestRate using the service's public methods
    private Optional<ExchangeRateDto> invokeSelectBestRate(List<ExchangeRateDto> rates) {
        // Use reflection to call private method
        try {
            var method = ExchangeRateAggregatorService.class.getDeclaredMethod("selectBestRate", List.class);
            method.setAccessible(true);
            return (Optional<ExchangeRateDto>) method.invoke(aggregatorService, rates);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke selectBestRate", e);
        }
    }

    private ExchangeRateDto createDto(String base, String target, BigDecimal rate, String provider) {
        return ExchangeRateDto.builder()
                .baseCurrency(base)
                .targetCurrency(target)
                .rate(rate)
                .provider(provider)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
