package com.paydue.api.dto;

import com.paydue.domain.UserRole;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String email,
        UserRole role,
        Long supplierId,
        Long buyerId
) {
    public static AuthResponse bearer(
            String token,
            Long userId,
            String email,
            UserRole role,
            Long supplierId,
            Long buyerId) {
        return new AuthResponse(token, "Bearer", userId, email, role, supplierId, buyerId);
    }
}
