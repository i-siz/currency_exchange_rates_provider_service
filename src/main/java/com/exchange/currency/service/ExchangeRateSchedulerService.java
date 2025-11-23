package com.exchange.currency.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Scheduled service for periodic fetching of exchange rates.
 * Fetches rates from all providers on startup and every hour.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateSchedulerService {

    private final ExchangeRateAggregatorService aggregatorService;
    private final CurrencyService currencyService;

    /**
     * Fetch exchange rates on application startup.
     * This ensures fresh rates are available immediately after deployment.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void fetchRatesOnStartup() {
        log.info("Application ready - starting initial exchange rate fetch");
        fetchAndUpdateRates();
    }

    /**
     * Scheduled task to fetch exchange rates every hour.
     * Cron expression: 0 0 * * * * = every hour at minute 0
     */
    @Scheduled(cron = "${exchange.scheduling.rate-update-cron:0 0 * * * *}")
    public void fetchRatesHourly() {
        log.info("Scheduled exchange rate fetch triggered");
        fetchAndUpdateRates();
    }

    /**
     * Fetch and update exchange rates for all supported currencies.
     * This method iterates through all currencies and fetches rates from all providers.
     */
    private void fetchAndUpdateRates() {
        try {
            List<String> availableProviders = aggregatorService.getAvailableProviders();
            if (log.isInfoEnabled()) {
                log.info("Available providers: {}", availableProviders);
            }

            if (availableProviders.isEmpty()) {
                log.warn("No exchange rate providers are available. Skipping rate update.");
                return;
            }

            // Get all supported currencies
            List<String> currencyCodes = currencyService.getAllCurrencies().stream()
                    .map(com.exchange.currency.dto.CurrencyDto::getCode)
                    .toList();

            if (currencyCodes.isEmpty()) {
                log.warn("No currencies configured in database. Skipping rate update.");
                return;
            }

            if (log.isInfoEnabled()) {
                log.info("Fetching rates for {} currencies", currencyCodes.size());
            }
            int totalSaved = 0;

            for (String baseCurrency : currencyCodes) {
                try {
                    int saved = aggregatorService.fetchAndSaveBestRates(baseCurrency);
                    totalSaved += saved;
                } catch (Exception ex) {
                    if (log.isErrorEnabled()) {
                        log.error("Error fetching rates for base currency {}: {}", 
                                baseCurrency, ex.getMessage(), ex);
                    }
                }
            }

            if (log.isInfoEnabled()) {
                log.info("Exchange rate update completed. Total rates saved: {}", totalSaved);
            }
        } catch (Exception ex) {
            if (log.isErrorEnabled()) {
                log.error("Error during scheduled exchange rate fetch: {}", ex.getMessage(), ex);
            }
        }
    }

    /**
     * Manual trigger for rate refresh (can be called from REST endpoint).
     */
    public void triggerManualRefresh() {
        log.info("Manual exchange rate refresh triggered");
        fetchAndUpdateRates();
    }
}
