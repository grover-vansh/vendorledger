package com.paydue.api.dto;

import java.time.Instant;

public record SupplierResponse(
        Long id,
        String name,
        String email,
        String gstin,
        boolean msme,
        Instant registeredAt,
        long productCount
) {
}
