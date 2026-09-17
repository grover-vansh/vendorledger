package com.paydue.api.dto;

import java.time.Instant;

public record BuyerResponse(
        Long id,
        Long supplierId,
        String name,
        String email,
        String gstin,
        String phone,
        String contactName,
        String billingAddress,
        Instant registeredAt
) {
}
