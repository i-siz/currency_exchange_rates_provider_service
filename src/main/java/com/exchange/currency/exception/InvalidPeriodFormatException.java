package com.exchange.currency.exception;

/**
 * Exception thrown when an invalid period format is provided for trends analysis.
 */
public class InvalidPeriodFormatException extends RuntimeException {

    public InvalidPeriodFormatException(String message) {
        super(message);
    }

    public InvalidPeriodFormatException(String period, String expectedFormat) {
        super(String.format("Invalid period format '%s'. Expected format: %s", period, expectedFormat));
    }
}
