package com.exchange.currency.exception;

/**
 * Exception thrown when an invalid currency code is provided.
 */
public class InvalidCurrencyException extends RuntimeException {

    public InvalidCurrencyException(String message) {
        super(message);
    }

    public InvalidCurrencyException(String currencyCode, String reason) {
        super(String.format("Invalid currency code '%s': %s", currencyCode, reason));
    }

    public InvalidCurrencyException(String currencyCode, String reason, Throwable cause) {
        super(String.format("Invalid currency code '%s': %s", currencyCode, reason), cause);
    }
}
