package com.monsterhouse.booking.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductOptionSaveRequest(

        @NotBlank
        @Size(max = 100)
        String nameKo,
        @Size(max = 100)
        String nameJa,
        @NotNull
        @DecimalMin("0")
        BigDecimal price,
        @Min(1)
        @Max(99)
        int maxQuantity,
        @Min(0)
        int sortOrder,
        boolean active
) {
}
