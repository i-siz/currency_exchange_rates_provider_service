package com.exchange.currency.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object for Exchange Rate calculation response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateResponseDto {

    private BigDecimal amount;
    private String from;
    private String to;
    private BigDecimal rate;
    private BigDecimal result;
}
