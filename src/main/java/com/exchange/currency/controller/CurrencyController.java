package com.exchange.currency.controller;

import com.exchange.currency.dto.CurrencyDto;
import com.exchange.currency.dto.ExchangeRateRequestDto;
import com.exchange.currency.dto.ExchangeRateResponseDto;
import com.exchange.currency.dto.TrendRequestDto;
import com.exchange.currency.dto.TrendResponseDto;
import com.exchange.currency.service.CurrencyService;
import com.exchange.currency.service.ExchangeRateSchedulerService;
import com.exchange.currency.service.ExchangeRateService;
import com.exchange.currency.service.TrendsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for currency exchange operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/currencies")
@RequiredArgsConstructor
@Validated
@Tag(name = "Currency Exchange", description = "Currency and exchange rate management APIs")
public class CurrencyController {

    private final CurrencyService currencyService;
    private final ExchangeRateService exchangeRateService;
    private final TrendsService trendsService;
    private final ExchangeRateSchedulerService schedulerService;

    @Operation(summary = "Get all currencies", description = "Retrieve list of all supported currencies")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved currencies")
    })
    @GetMapping
    public ResponseEntity<List<CurrencyDto>> getAllCurrencies() {
        if (log.isInfoEnabled()) {
            log.info("GET /api/v1/currencies - Get all currencies");
        }
        List<CurrencyDto> currencies = currencyService.getAllCurrencies();
        return ResponseEntity.ok(currencies);
    }

    @Operation(
            summary = "Add new currency",
            description = "Add a new currency to the system (ADMIN only)",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Currency added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid currency code"),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CurrencyDto> addCurrency(
            @Parameter(description = "Currency code (ISO 4217)", example = "EUR")
            @RequestParam
            @NotEmpty(message = "Currency code is required")
            @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be 3 uppercase letters")
            String currency) {
        if (log.isInfoEnabled()) {
            log.info("POST /api/v1/currencies - Add currency: {}", currency);
        }
        CurrencyDto created = currencyService.addCurrency(currency);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Calculate exchange rate",
            description = "Calculate the result of exchanging an amount from one currency to another"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exchange rate calculated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Currency or exchange rate not found")
    })
    @GetMapping("/exchange-rates")
    public ResponseEntity<ExchangeRateResponseDto> calculateExchangeRate(
            @Valid @ModelAttribute ExchangeRateRequestDto request) {
        if (log.isInfoEnabled()) {
            log.info("GET /api/v1/currencies/exchange-rates - Calculate: {} {} to {}", 
                    request.getAmount(), request.getFrom(), request.getTo());
        }
        ExchangeRateResponseDto response = exchangeRateService.calculateExchangeRate(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Refresh exchange rates",
            description = "Manually trigger exchange rate refresh from all providers (ADMIN only)",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exchange rates refreshed successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required")
    })
    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> refreshExchangeRates() {
        if (log.isInfoEnabled()) {
            log.info("POST /api/v1/currencies/refresh - Refresh exchange rates");
        }
        schedulerService.triggerManualRefresh();
        return ResponseEntity.ok("Exchange rates refresh initiated");
    }

    @Operation(
            summary = "Get exchange rate trends",
            description = "Analyze exchange rate trends over a specified period (ADMIN and PREMIUM_USER only)",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trends calculated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN or PREMIUM_USER role required"),
            @ApiResponse(responseCode = "404", description = "No data found for the specified period")
    })
    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('ADMIN', 'PREMIUM_USER')")
    public ResponseEntity<TrendResponseDto> getTrends(@Valid @ModelAttribute TrendRequestDto request) {
        if (log.isInfoEnabled()) {
            log.info("GET /api/v1/currencies/trends - Get trends: {} to {} over {}", 
                    request.getFrom(), request.getTo(), request.getPeriod());
        }
        TrendResponseDto trends = trendsService.calculateTrends(request);
        return ResponseEntity.ok(trends);
    }
}
