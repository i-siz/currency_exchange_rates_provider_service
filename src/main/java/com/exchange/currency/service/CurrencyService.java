package com.exchange.currency.service;

import com.exchange.currency.dto.CurrencyDto;
import com.exchange.currency.entity.Currency;
import com.exchange.currency.exception.CurrencyNotFoundException;
import com.exchange.currency.exception.InvalidCurrencyException;
import com.exchange.currency.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing currencies.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyRepository currencyRepository;

    /**
     * Get all available currencies.
     *
     * @return list of currency DTOs
     */
    @Transactional(readOnly = true)
    public List<CurrencyDto> getAllCurrencies() {
        log.debug("Fetching all currencies");
        return currencyRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Add a new currency.
     *
     * @param code currency code (ISO 4217)
     * @return created currency DTO
     */
    @Transactional
    public CurrencyDto addCurrency(String code) {
        log.info("Adding new currency: {}", code);
        
        // Validate currency code format
        if (code == null || !code.matches("^[A-Z]{3}$")) {
            throw new InvalidCurrencyException(code, "must be 3 uppercase letters (ISO 4217)");
        }

        // Check if currency already exists
        if (currencyRepository.existsByCode(code)) {
            throw new InvalidCurrencyException(code, "already exists");
        }

        // Validate against ISO 4217 standard
        String currencyName;
        try {
            java.util.Currency isoCurrency = java.util.Currency.getInstance(code);
            currencyName = isoCurrency.getDisplayName();
        } catch (IllegalArgumentException e) {
            throw new InvalidCurrencyException(code, "not a valid ISO 4217 currency code", e);
        }

        // Create and save new currency
        Currency currency = Currency.builder()
                .code(code)
                .name(currencyName)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Currency saved = currencyRepository.save(currency);
        log.info("Currency {} added successfully", code);
        
        return toDto(saved);
    }

    /**
     * Get currency by code.
     *
     * @param code currency code
     * @return currency entity
     */
    @Transactional(readOnly = true)
    public Currency getCurrencyByCode(String code) {
        return currencyRepository.findByCode(code)
                .orElseThrow(() -> new CurrencyNotFoundException(code, "not found in system"));
    }

    /**
     * Check if currency exists.
     *
     * @param code currency code
     * @return true if exists
     */
    public boolean currencyExists(String code) {
        return currencyRepository.existsByCode(code);
    }

    /**
     * Get all supported ISO 4217 currency codes.
     *
     * @return set of currency codes
     */
    public Set<String> getAllIsoCurrencyCodes() {
        return java.util.Currency.getAvailableCurrencies()
                .stream()
                .map(java.util.Currency::getCurrencyCode)
                .collect(Collectors.toSet());
    }

    /**
     * Convert entity to DTO.
     */
    private CurrencyDto toDto(Currency currency) {
        return CurrencyDto.builder()
                .code(currency.getCode())
                .name(currency.getName())
                .build();
    }
}
