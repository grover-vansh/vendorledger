package com.paydue.repo;

import com.paydue.domain.Invoice;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsBySupplierIdAndInvoiceNumber(Long supplierId, String invoiceNumber);

    @EntityGraph(attributePaths = {"lines", "lines.product", "supplier", "buyer"})
    List<Invoice> findBySupplierIdOrderByInvoiceDateDesc(Long supplierId);

    @EntityGraph(attributePaths = {"lines", "lines.product", "supplier", "buyer"})
    List<Invoice> findByBuyerIdOrderByInvoiceDateDesc(Long buyerId);
}
