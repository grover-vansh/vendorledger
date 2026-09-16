package com.paydue.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBuyerRequest(
        @NotBlank String name,
        String email,
        String gstin
) {
}
