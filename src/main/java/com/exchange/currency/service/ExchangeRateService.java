package com.exchange.currency.service;

import com.exchange.currency.dto.ExchangeRateDto;
import com.exchange.currency.dto.ExchangeRateRequestDto;
import com.exchange.currency.dto.ExchangeRateResponseDto;
import com.exchange.currency.entity.ExchangeRate;
import com.exchange.currency.exception.ExchangeRateNotFoundException;
import com.exchange.currency.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for managing exchange rates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyService currencyService;

    /**
     * Calculate exchange rate for given amount.
     *
     * @param request exchange rate request
     * @return exchange rate response with calculated result
     */
    @Transactional(readOnly = true)
    public ExchangeRateResponseDto calculateExchangeRate(ExchangeRateRequestDto request) {
        if (log.isDebugEnabled()) {
            log.debug("Calculating exchange rate: {} {} to {}", request.getAmount(), request.getFrom(), request.getTo());
        }

        // Validate currencies exist
        currencyService.getCurrencyByCode(request.getFrom());
        currencyService.getCurrencyByCode(request.getTo());

        // Get latest exchange rate
        BigDecimal rate = getLatestRate(request.getFrom(), request.getTo());

        // Calculate result
        BigDecimal result = request.getAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);

        return ExchangeRateResponseDto.builder()
                .amount(request.getAmount())
                .from(request.getFrom())
                .to(request.getTo())
                .rate(rate)
                .result(result)
                .build();
    }

    /**
     * Get latest exchange rate between two currencies.
     * Result is cached in Redis with key based on currency pair.
     *
     * @param baseCurrency base currency code
     * @param targetCurrency target currency code
     * @return exchange rate
     */
    @Cacheable(value = "exchangeRates", key = "#baseCurrency + ':' + #targetCurrency")
    @Transactional(readOnly = true)
    public BigDecimal getLatestRate(String baseCurrency, String targetCurrency) {
        // Handle same currency case
        if (baseCurrency.equals(targetCurrency)) {
            return BigDecimal.ONE;
        }

        Optional<ExchangeRate> rateOpt = exchangeRateRepository
                .findLatestByBaseCurrencyAndTargetCurrency(baseCurrency, targetCurrency);

        if (rateOpt.isPresent()) {
            return rateOpt.get().getRate();
        }

        // Try inverse rate
        Optional<ExchangeRate> inverseRateOpt = exchangeRateRepository
                .findLatestByBaseCurrencyAndTargetCurrency(targetCurrency, baseCurrency);

        if (inverseRateOpt.isPresent()) {
            // Calculate inverse rate: 1 / rate
            BigDecimal inverseRate = inverseRateOpt.get().getRate();
            return BigDecimal.ONE.divide(inverseRate, 6, RoundingMode.HALF_UP);
        }

        throw new ExchangeRateNotFoundException(baseCurrency, targetCurrency);
    }

    /**
     * Save exchange rate to database.
     * Evicts cache entry for this currency pair to force refresh on next query.
     *
     * @param exchangeRateDto exchange rate DTO
     */
    @CacheEvict(value = "exchangeRates", key = "#exchangeRateDto.baseCurrency + ':' + #exchangeRateDto.targetCurrency")
    @Transactional
    public void saveExchangeRate(ExchangeRateDto exchangeRateDto) {
        if (log.isDebugEnabled()) {
            log.debug("Saving exchange rate: {} {} to {} = {}", 
                    exchangeRateDto.getProvider(),
                    exchangeRateDto.getBaseCurrency(),
                    exchangeRateDto.getTargetCurrency(),
                    exchangeRateDto.getRate());
        }

        ExchangeRate exchangeRate = ExchangeRate.builder()
                .baseCurrency(exchangeRateDto.getBaseCurrency())
                .targetCurrency(exchangeRateDto.getTargetCurrency())
                .rate(exchangeRateDto.getRate())
                .provider(exchangeRateDto.getProvider())
                .timestamp(exchangeRateDto.getTimestamp())
                .createdAt(LocalDateTime.now())
                .build();

        exchangeRateRepository.save(exchangeRate);
    }
}
