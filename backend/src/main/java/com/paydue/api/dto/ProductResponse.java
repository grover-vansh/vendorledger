package com.paydue.api.dto;

import java.time.Instant;

public record ProductResponse(
        Long id,
        Long supplierId,
        String skuCode,
        String name,
        String productType,
        Integer shelfLifeDays,
        boolean active,
        Instant registeredAt
) {
}
