package com.exchange.currency.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "Base currency code (ISO 4217)", example = "USD")
    @NotEmpty(message = "Base currency (from) is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be 3 uppercase letters")
    private String from;

    @Schema(description = "Target currency code (ISO 4217)", example = "EUR")
    @NotEmpty(message = "Target currency (to) is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be 3 uppercase letters")
    private String to;

    @Schema(
        description = "Time period for trend analysis. Format: number + unit (H=hours, D=days, M=months, Y=years)",
        example = "7D",
        pattern = "^\\d+[HDMY]$"
    )
    @NotEmpty(message = "Period is required")
    @Pattern(
        regexp = "^\\d+[HDMY]$",
        message = "Period must be in format: number followed by H (hours), D (days), M (months), or Y (years), e.g., 12H, 7D, 3M, 1Y"
    )
    private String period;
}
