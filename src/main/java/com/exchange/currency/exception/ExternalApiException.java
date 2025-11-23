package com.exchange.currency.exception;

/**
 * Exception thrown when an external API call fails.
 */
public class ExternalApiException extends RuntimeException {

    private final String provider;

    public ExternalApiException(String provider, String message) {
        super(String.format("External API error from %s: %s", provider, message));
        this.provider = provider;
    }

    public ExternalApiException(String provider, String message, Throwable cause) {
        super(String.format("External API error from %s: %s", provider, message), cause);
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }
}
