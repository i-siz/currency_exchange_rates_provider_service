package com.exchange.currency.repository;

import com.exchange.currency.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Currency entity.
 */
@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    /**
     * Find currency by code.
     *
     * @param code the currency code (ISO 4217)
     * @return Optional containing the currency if found
     */
    Optional<Currency> findByCode(String code);

    /**
     * Check if currency exists by code.
     *
     * @param code the currency code (ISO 4217)
     * @return true if currency exists
     */
    boolean existsByCode(String code);
}
