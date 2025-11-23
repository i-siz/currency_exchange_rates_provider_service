package com.exchange.currency.exception;

/**
 * Exception thrown when a requested currency is not found.
 */
public class CurrencyNotFoundException extends RuntimeException {

    public CurrencyNotFoundException(String currencyCode, String message) {
        super(String.format("Currency '%s' not found: %s", currencyCode, message));
    }
}
