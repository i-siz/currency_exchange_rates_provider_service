package com.exchange.currency.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for Trend calculation response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendResponseDto {

    private String from;
    private String to;
    private String period;
    private BigDecimal startRate;
    private BigDecimal endRate;
    private BigDecimal change;
    private BigDecimal changePercent;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<RatePoint> dataPoints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatePoint {
        private LocalDateTime timestamp;
        private BigDecimal rate;
    }
}
