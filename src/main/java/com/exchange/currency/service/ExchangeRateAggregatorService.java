package com.exchange.currency.service;

import com.exchange.currency.dto.ExchangeRateDto;
import com.exchange.currency.provider.ExchangeRateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service that aggregates exchange rates from multiple providers.
 * Implements logic to select the best rate from available providers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateAggregatorService {

    private final List<ExchangeRateProvider> providers;
    private final ExchangeRateService exchangeRateService;

    /**
     * Fetch exchange rates for a base currency from all available providers.
     * Returns map of currency pairs to list of rates from different providers.
     *
     * @param baseCurrency base currency code
     * @return map of target currency -> list of exchange rate DTOs from different providers
     */
    public Map<String, List<ExchangeRateDto>> fetchFromAllProviders(String baseCurrency) {
        if (log.isInfoEnabled()) {
            log.info("Fetching exchange rates for {} from {} providers", baseCurrency, providers.size());
        }
        
        Map<String, List<ExchangeRateDto>> aggregatedRates = new HashMap<>();

        for (ExchangeRateProvider provider : providers) {
            if (!provider.isAvailable()) {
                if (log.isDebugEnabled()) {
                    log.debug("Provider {} is not available, skipping", provider.getProviderName());
                }
                continue;
            }

            try {
                Map<String, ExchangeRateDto> providerRates = provider.fetchExchangeRates(baseCurrency);
                
                providerRates.forEach((targetCurrency, rate) -> {
                    aggregatedRates.computeIfAbsent(targetCurrency, k -> new ArrayList<>()).add(rate);
                });

                if (log.isInfoEnabled()) {
                    log.info("Successfully fetched {} rates from {}", 
                            providerRates.size(), provider.getProviderName());
                }
            } catch (Exception ex) {
                if (log.isErrorEnabled()) {
                    log.error("Failed to fetch rates from {}: {}", 
                            provider.getProviderName(), ex.getMessage());
                }
            }
        }

        return aggregatedRates;
    }

    /**
     * Fetch and save best rates for a base currency.
     * Compares rates from all providers and saves the best rate to database.
     *
     * @param baseCurrency base currency code
     * @return number of rates saved
     */
    public int fetchAndSaveBestRates(String baseCurrency) {
        Map<String, List<ExchangeRateDto>> aggregatedRates = fetchFromAllProviders(baseCurrency);
        int savedCount = 0;

        for (Map.Entry<String, List<ExchangeRateDto>> entry : aggregatedRates.entrySet()) {
            String targetCurrency = entry.getKey();
            List<ExchangeRateDto> rates = entry.getValue();

            Optional<ExchangeRateDto> bestRate = selectBestRate(rates);
            
            if (bestRate.isPresent()) {
                try {
                    exchangeRateService.saveExchangeRate(bestRate.get());
                    savedCount++;
                    if (log.isDebugEnabled()) {
                        log.debug("Saved best rate for {} to {}: {} from {}", 
                                baseCurrency, targetCurrency, 
                                bestRate.get().getRate(), bestRate.get().getProvider());
                    }
                } catch (Exception ex) {
                    if (log.isErrorEnabled()) {
                        log.error("Failed to save rate for {} to {}: {}", 
                                baseCurrency, targetCurrency, ex.getMessage());
                    }
                }
            }
        }

        if (log.isInfoEnabled()) {
            log.info("Saved {} best rates for base currency {}", savedCount, baseCurrency);
        }
        return savedCount;
    }

    /**
     * Select the best rate from a list of rates from different providers.
     * Currently selects the middle rate to avoid outliers.
     * Alternative strategies: average, median, best for customer (highest for buy, lowest for sell).
     *
     * @param rates list of exchange rates from different providers
     * @return best exchange rate
     */
    private Optional<ExchangeRateDto> selectBestRate(List<ExchangeRateDto> rates) {
        if (rates == null || rates.isEmpty()) {
            return Optional.empty();
        }

        if (rates.size() == 1) {
            return Optional.of(rates.get(0));
        }

        // Sort rates by exchange rate value
        rates.sort((r1, r2) -> r1.getRate().compareTo(r2.getRate()));

        // Select median rate to avoid outliers
        int medianIndex = rates.size() / 2;
        return Optional.of(rates.get(medianIndex));
    }

    /**
     * Get list of available provider names.
     *
     * @return list of provider names
     */
    public List<String> getAvailableProviders() {
        return providers.stream()
                .filter(ExchangeRateProvider::isAvailable)
                .map(ExchangeRateProvider::getProviderName)
                .toList();
    }
}
