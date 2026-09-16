package com.paydue.api.dto;

import com.paydue.domain.UserRole;

public record UserSummaryResponse(
        Long userId,
        String email,
        UserRole role,
        Long supplierId,
        Long buyerId,
        String companyName
) {
}
