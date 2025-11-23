package com.exchange.currency.exception;

/**
 * Exception thrown when exchange rate information is not available.
 */
public class ExchangeRateNotFoundException extends RuntimeException {

    public ExchangeRateNotFoundException(String message) {
        super(message);
    }

    public ExchangeRateNotFoundException(String baseCurrency, String targetCurrency) {
        super(String.format("Exchange rate not found for %s to %s", baseCurrency, targetCurrency));
    }
}
