package com.exchange.currency.provider;

import com.exchange.currency.dto.ExchangeRateDto;

import java.util.Map;

/**
 * Interface for exchange rate providers.
 * Implementations can be real external APIs or mock services.
 */
public interface ExchangeRateProvider {

    /**
     * Get provider name/identifier.
     *
     * @return provider name
     */
    String getProviderName();

    /**
     * Fetch exchange rates for a base currency.
     * Returns map of target currency codes to exchange rates.
     *
     * @param baseCurrency base currency code (e.g., "USD")
     * @return map of target currency -> exchange rate DTO
     */
    Map<String, ExchangeRateDto> fetchExchangeRates(String baseCurrency);

    /**
     * Check if provider is available and responsive.
     *
     * @return true if provider is available
     */
    boolean isAvailable();
}
