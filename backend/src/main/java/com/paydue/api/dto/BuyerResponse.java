package com.paydue.api.dto;

import java.time.Instant;

public record BuyerResponse(
        Long id,
        String name,
        String email,
        String gstin,
        Instant registeredAt
) {
}
