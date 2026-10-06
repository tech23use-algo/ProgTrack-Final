package com.openspace.skillplanner.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateSkillPlanRequest(
        @NotNull UUID userId,
        @NotBlank
        @Size(min = 2, max = 80)
        @Pattern(regexp = "^[\\p{L}\\p{N} .,'&()#+-]{2,80}$",
                message = "must contain only letters, numbers, spaces, and basic punctuation")
        String skill,
        @Min(1) @Max(365) int days,
        @NotNull @DecimalMin("0.5") @DecimalMax("12.0") BigDecimal hoursPerDay) {
}
