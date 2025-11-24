package com.exchange.currency.integration;

import com.exchange.currency.service.CurrencyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security integration tests for API endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Security Integration Tests")
class SecurityIntegrationTest {

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
    private MockMvc mockMvc;

    @Autowired
    private CurrencyService currencyService;
    
    @Test
    @DisplayName("Public endpoint should be accessible without authentication")
    void publicEndpointShouldBeAccessibleWithoutAuth() throws Exception {
        // Add test currency if not exists
        if (!currencyService.currencyExists("USD")) {
            currencyService.addCurrency("USD");
        }

        // When & Then
        mockMvc.perform(get("/api/v1/currencies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Admin endpoint should return 401 without authentication")
    void adminEndpointShouldReturn401WithoutAuth() throws Exception {
        mockMvc.perform(post("/api/v1/currencies")
                        .param("currency", "EUR")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin endpoint should be accessible with ADMIN role")
    void adminEndpointShouldBeAccessibleWithAdminRole() throws Exception {
        mockMvc.perform(post("/api/v1/currencies")
                        .param("currency", "GBP")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("GBP"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Admin endpoint should return 403 with USER role")
    void adminEndpointShouldReturn403WithUserRole() throws Exception {
        mockMvc.perform(post("/api/v1/currencies")
                        .param("currency", "JPY")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PREMIUM_USER")
    @DisplayName("Admin endpoint should return 403 with PREMIUM_USER role")
    void adminEndpointShouldReturn403WithPremiumUserRole() throws Exception {
        mockMvc.perform(post("/api/v1/currencies")
                        .param("currency", "CHF")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should authenticate with HTTP Basic Auth - admin user")
    void shouldAuthenticateWithHttpBasicAuthAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/currencies")
                        .param("currency", "AUD")
                        .with(httpBasic("admin", "admin123"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("AUD"));
    }

    @Test
    @DisplayName("Should reject invalid credentials")
    void shouldRejectInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/currencies")
                        .param("currency", "CAD")
                        .with(httpBasic("admin", "wrongpassword"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PREMIUM_USER"})
    @DisplayName("Trends endpoint should be accessible with ADMIN role")
    void trendsEndpointShouldBeAccessibleWithAdminRole() throws Exception {
        // Add required currencies if not exists
        if (!currencyService.currencyExists("USD")) {
            currencyService.addCurrency("USD");
        }
        if (!currencyService.currencyExists("EUR")) {
            currencyService.addCurrency("EUR");
        }

        mockMvc.perform(get("/api/v1/currencies/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "12H")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // No exchange rates exist yet
    }

    @Test
    @WithMockUser(roles = "PREMIUM_USER")
    @DisplayName("Trends endpoint should be accessible with PREMIUM_USER role")
    void trendsEndpointShouldBeAccessibleWithPremiumUserRole() throws Exception {
        // Add required currencies if not exists
        if (!currencyService.currencyExists("USD")) {
            currencyService.addCurrency("USD");
        }
        if (!currencyService.currencyExists("EUR")) {
            currencyService.addCurrency("EUR");
        }

        mockMvc.perform(get("/api/v1/currencies/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "12H")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // No exchange rates exist yet
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Trends endpoint should return 403 with USER role")
    void trendsEndpointShouldReturn403WithUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/currencies/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "12H")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Exchange rates endpoint should be accessible without authentication")
    void exchangeRatesEndpointShouldBeAccessibleWithoutAuth() throws Exception {
        // Add currencies if not exist
        if (!currencyService.currencyExists("USD")) {
            currencyService.addCurrency("USD");
        }
        if (!currencyService.currencyExists("EUR")) {
            currencyService.addCurrency("EUR");
        }

        mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                        .param("amount", "100")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()); // Exchange rates exist from refresh call
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Refresh endpoint should be accessible with ADMIN role")
    void refreshEndpointShouldBeAccessibleWithAdminRole() throws Exception {
        mockMvc.perform(post("/api/v1/currencies/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Refresh endpoint should return 403 with USER role")
    void refreshEndpointShouldReturn403WithUserRole() throws Exception {
        mockMvc.perform(post("/api/v1/currencies/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
