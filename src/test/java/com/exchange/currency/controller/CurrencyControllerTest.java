package com.exchange.currency.controller;

import com.exchange.currency.dto.CurrencyDto;
import com.exchange.currency.exception.InvalidCurrencyException;
import com.exchange.currency.service.CurrencyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CurrencyController.class)
@Import(TestSecurityConfig.class)
@DisplayName("CurrencyController Tests")
class CurrencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrencyService currencyService;

    @MockitoBean
    private com.exchange.currency.service.ExchangeRateService exchangeRateService;

    @MockitoBean
    private com.exchange.currency.service.ExchangeRateSchedulerService schedulerService;

    @MockitoBean
    private com.exchange.currency.service.TrendsService trendsService;

    @Test
    @DisplayName("GET /api/v1/currencies should return all currencies")
    void getAllCurrencies_ShouldReturnList() throws Exception {
        // Given
        List<CurrencyDto> currencies = Arrays.asList(
                CurrencyDto.builder().code("USD").name("US Dollar").build(),
                CurrencyDto.builder().code("EUR").name("Euro").build()
        );
        when(currencyService.getAllCurrencies()).thenReturn(currencies);

        // When & Then
        mockMvc.perform(get("/api/v1/currencies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].code").value("USD"))
                .andExpect(jsonPath("$[0].name").value("US Dollar"))
                .andExpect(jsonPath("$[1].code").value("EUR"))
                .andExpect(jsonPath("$[1].name").value("Euro"));

        verify(currencyService, times(1)).getAllCurrencies();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/currencies should add currency with ADMIN role")
    void addCurrency_ShouldAddCurrency_WithAdminRole() throws Exception {
        // Given
        CurrencyDto currency = CurrencyDto.builder()
                .code("GBP")
                .name("British Pound")
                .build();
        when(currencyService.addCurrency("GBP")).thenReturn(currency);

        // When & Then
        mockMvc.perform(post("/api/v1/currencies")
                        .param("currency", "GBP")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("GBP"))
                .andExpect(jsonPath("$.name").value("British Pound"));

        verify(currencyService, times(1)).addCurrency("GBP");
    }

    @Test
    @DisplayName("POST /api/v1/currencies should return 403 without authentication")
    void addCurrency_ShouldReturn403_WithoutAuth() throws Exception {
        // When & Then
        // Anonymous users get 403 (Forbidden) with @PreAuthorize, not 401 (Unauthorized)
        mockMvc.perform(post("/api/v1/currencies")
                        .param("currency", "GBP")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(currencyService, never()).addCurrency(anyString());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/currencies should return 403 with USER role")
    void addCurrency_ShouldReturn403_WithUserRole() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/currencies")
                        .param("currency", "GBP")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(currencyService, never()).addCurrency(anyString());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/currencies should return 400 for empty currency")
    void addCurrency_ShouldReturn400_WhenCurrencyEmpty() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/currencies")
                        .param("currency", "")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(currencyService, never()).addCurrency(anyString());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/currencies should return 400 for invalid format")
    void addCurrency_ShouldReturn400_WhenInvalidFormat() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/currencies")
                        .param("currency", "US")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(currencyService, never()).addCurrency(anyString());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/currencies should return 400 when currency exists")
    void addCurrency_ShouldReturn400_WhenCurrencyExists() throws Exception {
        // Given
        when(currencyService.addCurrency("USD"))
                .thenThrow(new InvalidCurrencyException("USD", "already exists"));

        // When & Then
        mockMvc.perform(post("/api/v1/currencies")
                        .param("currency", "USD")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid currency code 'USD': already exists"));

        verify(currencyService, times(1)).addCurrency("USD");
    }
}
