package com.exchange.currency.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Trend calculation request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendRequestDto {

    @NotEmpty(message = "Base currency (from) is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be 3 uppercase letters")
    private String from;

    @NotEmpty(message = "Target currency (to) is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be 3 uppercase letters")
    private String to;

    @NotEmpty(message = "Period is required")
    @Pattern(
        regexp = "^\\d+[HDMY]$",
        message = "Period must be in format: number followed by H (hours), D (days), M (months), or Y (years), e.g., 12H, 10D, 3M, 1Y"
    )
    private String period;
}
