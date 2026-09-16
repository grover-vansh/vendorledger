package com.paydue.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSupplierRequest(
        @NotBlank String name,
        String email,
        String gstin,
        Boolean msme
) {
}
