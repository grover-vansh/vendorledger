package com.paydue.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceResponse(
        Long id,
        Long supplierId,
        String supplierName,
        Long buyerId,
        String buyerName,
        String invoiceNumber,
        LocalDate invoiceDate,
        LocalDate dueDate,
        String status,
        BigDecimal totalAmount,
        long daysOverdue,
        List<InvoiceLineResponse> lines
) {
    public record InvoiceLineResponse(
            Long id,
            Long productId,
            String skuCode,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }
}
