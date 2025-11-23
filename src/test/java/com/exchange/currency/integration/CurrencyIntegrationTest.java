package com.exchange.currency.integration;

import com.exchange.currency.dto.CurrencyDto;
import com.exchange.currency.entity.Currency;
import com.exchange.currency.repository.CurrencyRepository;
import com.exchange.currency.service.CurrencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Currency functionality using TestContainers.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Currency Integration Tests")
class CurrencyIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.liquibase.enabled", () -> "true");
    }

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private CurrencyRepository currencyRepository;

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        currencyRepository.deleteAll();
    }

    @Test
    @DisplayName("Should persist and retrieve currency from database")
    void shouldPersistAndRetrieveCurrency() {
        // Given
        String currencyCode = "USD";

        // When
        CurrencyDto savedDto = currencyService.addCurrency(currencyCode);

        // Then
        assertThat(savedDto).isNotNull();
        assertThat(savedDto.getCode()).isEqualTo(currencyCode);
        assertThat(savedDto.getName()).isEqualTo("US Dollar");

        // Verify it's actually in the database
        List<Currency> allCurrencies = currencyRepository.findAll();
        assertThat(allCurrencies).hasSize(1);
        assertThat(allCurrencies.get(0).getCode()).isEqualTo(currencyCode);
    }

    @Test
    @DisplayName("Should retrieve all currencies from database")
    void shouldRetrieveAllCurrencies() {
        // Given
        currencyService.addCurrency("USD");
        currencyService.addCurrency("EUR");
        currencyService.addCurrency("GBP");

        // When
        List<CurrencyDto> currencies = currencyService.getAllCurrencies();

        // Then
        assertThat(currencies).hasSize(3);
        assertThat(currencies)
                .extracting(CurrencyDto::getCode)
                .containsExactlyInAnyOrder("USD", "EUR", "GBP");
    }

    @Test
    @DisplayName("Should check currency existence in database")
    void shouldCheckCurrencyExistence() {
        // Given
        currencyService.addCurrency("JPY");

        // When
        boolean exists = currencyService.currencyExists("JPY");
        boolean notExists = currencyService.currencyExists("CHF");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should handle Liquibase migrations correctly")
    void shouldHandleLiquibaseMigrations() {
        // This test verifies that Liquibase migrations run successfully
        // by checking that we can interact with the database

        // When
        long count = currencyRepository.count();

        // Then - should not throw exception and database should be accessible
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should support transaction rollback on error")
    void shouldSupportTransactionRollback() {
        // Given
        currencyService.addCurrency("AUD");
        long initialCount = currencyRepository.count();

        // When - try to add invalid currency (should fail)
        try {
            currencyService.addCurrency("INVALID");
        } catch (Exception e) {
            // Expected
        }

        // Then - count should remain the same (transaction rolled back)
        long finalCount = currencyRepository.count();
        assertThat(finalCount).isEqualTo(initialCount);
    }
}
