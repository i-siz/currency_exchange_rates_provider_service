package com.exchange.currency.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Exchange Rate information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateDto {

    private String baseCurrency;
    private String targetCurrency;
    private BigDecimal rate;
    private String provider;
    private LocalDateTime timestamp;
}
