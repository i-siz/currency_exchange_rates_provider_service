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
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GlobalExceptionHandler.
 * Focus on killing SURVIVED and NO_COVERAGE mutations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }

    @Test
    @DisplayName("handleCurrencyNotFoundException should return 404 with non-null error response")
    void handleCurrencyNotFoundException_ShouldReturnNotFound() {
        // Given
        CurrencyNotFoundException exception = new CurrencyNotFoundException("USD", "not found in database");

        // When
        ErrorResponse response = exceptionHandler.handleCurrencyNotFoundException(exception, request);

        // Then - kills NullReturn mutation
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getMessage()).contains("USD");
        assertThat(response.getPath()).isEqualTo("/api/v1/test");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("handleExchangeRateNotFoundException should return 404 with non-null error response")
    void handleExchangeRateNotFoundException_ShouldReturnNotFound() {
        // Given
        ExchangeRateNotFoundException exception = new ExchangeRateNotFoundException("USD", "EUR");

        // When
        ErrorResponse response = exceptionHandler.handleExchangeRateNotFoundException(exception, request);

        // Then - kills NullReturn mutation
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getMessage()).contains("USD").contains("EUR");
        assertThat(response.getPath()).isEqualTo("/api/v1/test");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("handleInvalidCurrencyException should return 400 with non-null error response")
    void handleInvalidCurrencyException_ShouldReturnBadRequest() {
        // Given
        InvalidCurrencyException exception = new InvalidCurrencyException("XYZ", "not a valid ISO code");

        // When
        ErrorResponse response = exceptionHandler.handleInvalidCurrencyException(exception, request);

        // Then - kills NullReturn mutation
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getMessage()).contains("XYZ");
        assertThat(response.getPath()).isEqualTo("/api/v1/test");
    }

    @Test
    @DisplayName("handleInvalidPeriodFormatException should return 400 with non-null error response")
    void handleInvalidPeriodFormatException_ShouldReturnBadRequest() {
        // Given
        InvalidPeriodFormatException exception = new InvalidPeriodFormatException("invalid format");

        // When
        ErrorResponse response = exceptionHandler.handleInvalidPeriodFormatException(exception, request);

        // Then - kills NullReturn mutation
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getMessage()).contains("invalid format");
        assertThat(response.getPath()).isEqualTo("/api/v1/test");
    }

    @Test
    @DisplayName("handleExternalApiException should return 503 with non-null error response")
    void handleExternalApiException_ShouldReturnServiceUnavailable() {
        // Given
        ExternalApiException exception = new ExternalApiException("fixer.io", "Connection timeout");

        // When
        ErrorResponse response = exceptionHandler.handleExternalApiException(exception, request);

        // Then - kills NullReturn mutation
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(response.getMessage()).contains("fixer.io");
        assertThat(response.getPath()).isEqualTo("/api/v1/test");
    }

    @Test
    @DisplayName("handleValidationException should return 400 with field errors and non-null response")
    void handleValidationException_ShouldReturnBadRequestWithFieldErrors() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        
        FieldError fieldError = new FieldError("currencyDto", "code", "ABC", false, null, null, "must be 3 characters");
        ObjectError objectError = new ObjectError("currencyDto", "invalid object");
        
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError, objectError));

        // When
        ErrorResponse response = exceptionHandler.handleValidationException(exception, request);

        // Then - kills NullReturn mutation in lambda$handleValidationException$0
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getMessage()).isEqualTo("Validation failed");
        assertThat(response.getPath()).isEqualTo("/api/v1/test");
        assertThat(response.getFieldErrors()).isNotNull();
        assertThat(response.getFieldErrors()).hasSize(2);
        
        // Verify field error mapping (kills lambda mutations)
        ErrorResponse.FieldError mappedFieldError = response.getFieldErrors().get(0);
        assertThat(mappedFieldError).isNotNull(); // Kills NullReturn in lambda
        assertThat(mappedFieldError.getField()).isEqualTo("code");
        assertThat(mappedFieldError.getMessage()).isEqualTo("must be 3 characters");
        assertThat(mappedFieldError.getRejectedValue()).isEqualTo("ABC");
        
        // Verify object error mapping
        ErrorResponse.FieldError mappedObjectError = response.getFieldErrors().get(1);
        assertThat(mappedObjectError).isNotNull();
        assertThat(mappedObjectError.getField()).isEqualTo("currencyDto");
    }

    @Test
    @DisplayName("handleConstraintViolationException should return 400 with constraint violations and non-null response")
    void handleConstraintViolationException_ShouldReturnBadRequestWithViolations() {
        // Given
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path propertyPath = mock(Path.class);
        
        when(violation.getPropertyPath()).thenReturn(propertyPath);
        when(propertyPath.toString()).thenReturn("currency");
        when(violation.getMessage()).thenReturn("must not be empty");
        when(violation.getInvalidValue()).thenReturn("");
        
        violations.add(violation);
        ConstraintViolationException exception = new ConstraintViolationException(violations);

        // When
        ErrorResponse response = exceptionHandler.handleConstraintViolationException(exception, request);

        // Then - kills SURVIVED NullReturn mutation (line 141)
        assertThat(response).isNotNull(); // Critical assertion to kill mutation
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getMessage()).isEqualTo("must not be empty");
        assertThat(response.getPath()).isEqualTo("/api/v1/test");
        assertThat(response.getFieldErrors()).isNotNull();
        assertThat(response.getFieldErrors()).hasSize(1);
        
        // Verify lambda mapping (kills lambda$handleConstraintViolationException$1 mutation line 129)
        ErrorResponse.FieldError fieldError = response.getFieldErrors().get(0);
        assertThat(fieldError).isNotNull(); // Critical assertion to kill lambda NullReturn mutation
        assertThat(fieldError.getField()).isEqualTo("currency");
        assertThat(fieldError.getMessage()).isEqualTo("must not be empty");
        assertThat(fieldError.getRejectedValue()).isEqualTo("");
    }

    @Test
    @DisplayName("handleAuthenticationException should return 401 with non-null error response")
    void handleAuthenticationException_ShouldReturnUnauthorized() {
        // Given
        AuthenticationException exception = mock(AuthenticationException.class);
        when(exception.getMessage()).thenReturn("Bad credentials");

        // When
        ErrorResponse response = exceptionHandler.handleAuthenticationException(exception, request);

        // Then - kills NullReturn mutation (line 158)
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getMessage()).isEqualTo("Authentication required");
        assertThat(response.getPath()).isEqualTo("/api/v1/test");
        assertThat(response.getError()).isEqualTo("Unauthorized");
    }

    @Test
    @DisplayName("handleAccessDeniedException should return 403 with non-null error response")
    void handleAccessDeniedException_ShouldReturnForbidden() {
        // Given
        AccessDeniedException exception = new AccessDeniedException("Access is denied");

        // When
        ErrorResponse response = exceptionHandler.handleAccessDeniedException(exception, request);

        // Then - kills SURVIVED NullReturn mutation (line 169)
        assertThat(response).isNotNull(); // Critical assertion to kill mutation
        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getMessage()).isEqualTo("Access denied");
        assertThat(response.getPath()).isEqualTo("/api/v1/test");
        assertThat(response.getError()).isEqualTo("Forbidden");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("handleGenericException should return 500 with non-null error response")
    void handleGenericException_ShouldReturnInternalServerError() {
        // Given
        Exception exception = new RuntimeException("Unexpected error");

        // When
        ErrorResponse response = exceptionHandler.handleGenericException(exception, request);

        // Then - kills NullReturn mutation (line 180)
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getPath()).isEqualTo("/api/v1/test");
        assertThat(response.getError()).isEqualTo("Internal Server Error");
    }
}
