package com.exchange.currency.service;

import com.exchange.currency.dto.TrendRequestDto;
import com.exchange.currency.dto.TrendResponseDto;
import com.exchange.currency.entity.ExchangeRate;
import com.exchange.currency.exception.ExchangeRateNotFoundException;
import com.exchange.currency.exception.InvalidPeriodFormatException;
import com.exchange.currency.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for analyzing exchange rate trends.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrendsService {

    private static final Pattern PERIOD_PATTERN = Pattern.compile("^(\\d+)([HDMY])$");
    
    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyService currencyService;

    /**
     * Calculate exchange rate trends for given period.
     *
     * @param request trend request
     * @return trend response with analysis
     */
    @Transactional(readOnly = true)
    public TrendResponseDto calculateTrends(TrendRequestDto request) {
        if (log.isDebugEnabled()) {
            log.debug("Calculating trends for {} to {} over period {}", 
                    request.getFrom(), request.getTo(), request.getPeriod());
        }

        // Validate currencies exist
        currencyService.getCurrencyByCode(request.getFrom());
        currencyService.getCurrencyByCode(request.getTo());

        // Parse period
        PeriodInfo periodInfo = parsePeriod(request.getPeriod());
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = calculateStartTime(endTime, periodInfo);

        if (log.isDebugEnabled()) {
            log.debug("Period range: {} to {}", startTime, endTime);
        }

        // Fetch exchange rates for the period
        List<ExchangeRate> rates = exchangeRateRepository
                .findByBaseCurrencyAndTargetCurrencyAndTimestampBetween(
                        request.getFrom(), request.getTo(), startTime, endTime);

        if (rates.isEmpty()) {
            throw new ExchangeRateNotFoundException(
                    String.format("No exchange rate data found for %s to %s in period %s",
                            request.getFrom(), request.getTo(), request.getPeriod()));
        }

        // Get start and end rates
        ExchangeRate startRate = rates.get(0);
        ExchangeRate endRate = rates.get(rates.size() - 1);

        // Calculate change
        BigDecimal change = endRate.getRate().subtract(startRate.getRate());
        BigDecimal changePercent = change
                .divide(startRate.getRate(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        // Create data points
        List<TrendResponseDto.RatePoint> dataPoints = rates.stream()
                .map(rate -> TrendResponseDto.RatePoint.builder()
                        .timestamp(rate.getTimestamp())
                        .rate(rate.getRate())
                        .build())
                .collect(Collectors.toList());

        return TrendResponseDto.builder()
                .from(request.getFrom())
                .to(request.getTo())
                .period(request.getPeriod())
                .startRate(startRate.getRate())
                .endRate(endRate.getRate())
                .change(change)
                .changePercent(changePercent)
                .startTime(startRate.getTimestamp())
                .endTime(endRate.getTimestamp())
                .dataPoints(dataPoints)
                .build();
    }

    /**
     * Parse period string to extract amount and unit.
     */
    private PeriodInfo parsePeriod(String period) {
        Matcher matcher = PERIOD_PATTERN.matcher(period);
        if (!matcher.matches()) {
            throw new InvalidPeriodFormatException(period, 
                    "format: number followed by H (hours), D (days), M (months), or Y (years)");
        }

        int amount = Integer.parseInt(matcher.group(1));
        String unit = matcher.group(2);

        return new PeriodInfo(amount, unit);
    }

    /**
     * Calculate start time based on period.
     */
    private LocalDateTime calculateStartTime(LocalDateTime endTime, PeriodInfo periodInfo) {
        return switch (periodInfo.unit) {
            case "H" -> endTime.minusHours(periodInfo.amount);
            case "D" -> endTime.minusDays(periodInfo.amount);
            case "M" -> endTime.minusMonths(periodInfo.amount);
            case "Y" -> endTime.minusYears(periodInfo.amount);
            default -> throw new InvalidPeriodFormatException(periodInfo.unit, 
                    "valid units are: H (hours), D (days), M (months), Y (years)");
        };
    }

    /**
     * Internal class to hold period information.
     */
    private record PeriodInfo(int amount, String unit) {
    }
}
