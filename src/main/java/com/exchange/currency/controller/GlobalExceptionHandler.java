package com.exchange.currency.controller;

import com.exchange.currency.exception.CurrencyNotFoundException;
import com.exchange.currency.exception.ErrorResponse;
import com.exchange.currency.exception.ExchangeRateNotFoundException;
import com.exchange.currency.exception.ExternalApiException;
import com.exchange.currency.exception.InvalidCurrencyException;
import com.exchange.currency.exception.InvalidPeriodFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST controllers.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CurrencyNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleCurrencyNotFoundException(
            CurrencyNotFoundException ex, HttpServletRequest request) {
        if (log.isErrorEnabled()) {
            log.error("Currency not found: {}", ex.getMessage());
        }
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ExchangeRateNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleExchangeRateNotFoundException(
            ExchangeRateNotFoundException ex, HttpServletRequest request) {
        if (log.isErrorEnabled()) {
            log.error("Exchange rate not found: {}", ex.getMessage());
        }
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCurrencyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidCurrencyException(
            InvalidCurrencyException ex, HttpServletRequest request) {
        if (log.isErrorEnabled()) {
            log.error("Invalid currency: {}", ex.getMessage());
        }
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidPeriodFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidPeriodFormatException(
            InvalidPeriodFormatException ex, HttpServletRequest request) {
        if (log.isErrorEnabled()) {
            log.error("Invalid period format: {}", ex.getMessage());
        }
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(ExternalApiException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleExternalApiException(
            ExternalApiException ex, HttpServletRequest request) {
        if (log.isErrorEnabled()) {
            log.error("External API error: {}", ex.getMessage(), ex);
        }
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        if (log.isErrorEnabled()) {
            log.error("Validation error: {}", ex.getMessage());
        }

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    String fieldName = error instanceof FieldError
                            ? ((FieldError) error).getField() : error.getObjectName();
                    Object rejectedValue = error instanceof FieldError
                            ? ((FieldError) error).getRejectedValue() : null;
                    return ErrorResponse.FieldError.builder()
                            .field(fieldName)
                            .message(error.getDefaultMessage())
                            .rejectedValue(rejectedValue)
                            .build();
                })
                .collect(Collectors.toList());

        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        if (log.isErrorEnabled()) {
            log.error("Constraint violation: {}", ex.getMessage());
        }

        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> {
                    String fieldName = violation.getPropertyPath().toString();
                    return ErrorResponse.FieldError.builder()
                            .field(fieldName)
                            .message(violation.getMessage())
                            .rejectedValue(violation.getInvalidValue())
                            .build();
                })
                .collect(Collectors.toList());

        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        if (log.isErrorEnabled()) {
            log.error("Authentication error: {}", ex.getMessage());
        }
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, 
                "Authentication required", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        if (log.isErrorEnabled()) {
            log.error("Access denied: {}", ex.getMessage());
        }
        return buildErrorResponse(HttpStatus.FORBIDDEN, 
                "Access denied", request);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericException(
            Exception ex, HttpServletRequest request) {
        if (log.isErrorEnabled()) {
            log.error("Unexpected error occurred", ex);
        }
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "An unexpected error occurred", request);
    }

    private ErrorResponse buildErrorResponse(HttpStatus status, String message, 
                                            HttpServletRequest request) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();
    }
}
