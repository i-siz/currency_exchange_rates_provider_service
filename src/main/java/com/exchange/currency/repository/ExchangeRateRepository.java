package com.exchange.currency.repository;

import com.exchange.currency.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ExchangeRate entity.
 */
@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    /**
     * Find the latest exchange rate for a currency pair.
     *
     * @param baseCurrency   the base currency code
     * @param targetCurrency the target currency code
     * @return Optional containing the latest exchange rate if found
     */
    @Query("SELECT e FROM ExchangeRate e WHERE e.baseCurrency = :base AND e.targetCurrency = :target ORDER BY e.timestamp DESC LIMIT 1")
    Optional<ExchangeRate> findLatestByBaseCurrencyAndTargetCurrency(
            @Param("base") String baseCurrency,
            @Param("target") String targetCurrency
    );

    /**
     * Find exchange rates for a currency pair within a time period.
     *
     * @param baseCurrency   the base currency code
     * @param targetCurrency the target currency code
     * @param startTime      the start of the time period
     * @param endTime        the end of the time period
     * @return list of exchange rates within the time period
     */
    @Query("SELECT e FROM ExchangeRate e WHERE e.baseCurrency = :base AND e.targetCurrency = :target AND e.timestamp BETWEEN :start AND :end ORDER BY e.timestamp")
    List<ExchangeRate> findByBaseCurrencyAndTargetCurrencyAndTimestampBetween(
            @Param("base") String baseCurrency,
            @Param("target") String targetCurrency,
            @Param("start") LocalDateTime startTime,
            @Param("end") LocalDateTime endTime
    );
}
