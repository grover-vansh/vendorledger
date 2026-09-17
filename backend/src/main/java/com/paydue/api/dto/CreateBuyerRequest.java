package com.paydue.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBuyerRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 15) String gstin,
        @Size(max = 20) String phone,
        @Size(max = 200) String contactName,
        @Size(max = 500) String billingAddress
) {
}
