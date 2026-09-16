package com.paydue.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateInvoiceRequest(
        @NotNull Long supplierId,
        @NotNull Long buyerId,
        @NotBlank String invoiceNumber,
        @NotNull LocalDate invoiceDate,
        LocalDate dueDate,
        @NotEmpty @Valid List<CreateInvoiceLineRequest> lines
) {
    public record CreateInvoiceLineRequest(
            @NotNull Long productId,
            @NotNull BigDecimal quantity,
            @NotNull BigDecimal unitPrice
    ) {
    }
}
