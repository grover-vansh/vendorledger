package com.paydue.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateProductRequest(
        @NotBlank String skuCode,
        @NotBlank String name,
        String productType,
        Integer shelfLifeDays
) {
}
