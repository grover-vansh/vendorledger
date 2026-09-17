package com.paydue.service;

import com.paydue.api.dto.BuyerResponse;
import com.paydue.api.dto.CreateBuyerRequest;
import com.paydue.api.dto.CreateInvoiceRequest;
import com.paydue.api.dto.CreateProductRequest;
import com.paydue.api.dto.CreateSupplierRequest;
import com.paydue.api.dto.InvoiceResponse;
import com.paydue.api.dto.ProductResponse;
import com.paydue.api.dto.SupplierResponse;
import com.paydue.domain.Buyer;
import com.paydue.domain.Invoice;
import com.paydue.domain.InvoiceLine;
import com.paydue.domain.Product;
import com.paydue.domain.Supplier;
import com.paydue.repo.BuyerRepository;
import com.paydue.repo.InvoiceRepository;
import com.paydue.repo.ProductRepository;
import com.paydue.repo.SupplierRepository;
import com.paydue.web.error.ConflictException;
import com.paydue.web.error.NotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaydueService {

    private final SupplierRepository suppliers;
    private final BuyerRepository buyers;
    private final ProductRepository products;
    private final InvoiceRepository invoices;

    public PaydueService(
            SupplierRepository suppliers,
            BuyerRepository buyers,
            ProductRepository products,
            InvoiceRepository invoices) {
        this.suppliers = suppliers;
        this.buyers = buyers;
        this.products = products;
        this.invoices = invoices;
    }

    @Transactional
    public SupplierResponse createSupplier(CreateSupplierRequest req) {
        Supplier s = new Supplier();
        s.setName(req.name().trim());
        s.setEmail(req.email());
        s.setGstin(req.gstin());
        if (req.msme() != null) {
            s.setMsme(req.msme());
        }
        return toSupplier(suppliers.save(s));
    }

    @Transactional(readOnly = true)
    public SupplierResponse getSupplier(Long id) {
        return toSupplier(requireSupplier(id));
    }

    @Transactional
    public ProductResponse addProduct(Long supplierId, CreateProductRequest req) {
        Supplier supplier = requireSupplier(supplierId);
        if (products.existsBySupplierIdAndSkuCode(supplierId, req.skuCode())) {
            throw new ConflictException("SKU already exists for this supplier: " + req.skuCode());
        }
        Product p = new Product();
        p.setSupplier(supplier);
        p.setSkuCode(req.skuCode().trim());
        p.setName(req.name().trim());
        p.setProductType(req.productType() == null || req.productType().isBlank() ? "OTHER" : req.productType());
        p.setShelfLifeDays(req.shelfLifeDays());
        return toProduct(products.save(p));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts(Long supplierId) {
        requireSupplier(supplierId);
        return products.findBySupplierIdOrderByIdAsc(supplierId).stream().map(this::toProduct).toList();
    }

    @Transactional
    public BuyerResponse createBuyer(Long supplierId, CreateBuyerRequest req) {
        Supplier supplier = requireSupplier(supplierId);
        String name = req.name().trim();
        String email = req.email().trim().toLowerCase();
        String gstin = normalizeGstin(req.gstin());
        String phone = trimToNull(req.phone());
        String contactName = trimToNull(req.contactName());
        String billingAddress = trimToNull(req.billingAddress());

        if (buyers.existsBySupplierIdAndNameIgnoreCase(supplierId, name)) {
            throw new ConflictException("Buyer already exists for this seller: " + name);
        }
        if (buyers.existsBySupplierIdAndEmailIgnoreCase(supplierId, email)) {
            throw new ConflictException("Buyer email already exists for this seller");
        }
        if (gstin != null && buyers.existsBySupplierIdAndGstinIgnoreCase(supplierId, gstin)) {
            throw new ConflictException("Buyer GSTIN already exists for this seller");
        }

        Buyer b = new Buyer();
        b.setSupplier(supplier);
        b.setName(name);
        b.setEmail(email);
        b.setGstin(gstin);
        b.setPhone(phone);
        b.setContactName(contactName);
        b.setBillingAddress(billingAddress);
        return toBuyer(buyers.save(b));
    }

    @Transactional(readOnly = true)
    public List<BuyerResponse> listMyBuyers(Long supplierId) {
        requireSupplier(supplierId);
        return buyers.findBySupplierIdOrderByNameAsc(supplierId).stream()
                .map(this::toBuyer)
                .toList();
    }

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest req) {
        Supplier supplier = requireSupplier(req.supplierId());
        Buyer buyer = buyers.findByIdAndSupplierId(req.buyerId(), req.supplierId())
                .orElseThrow(() -> new NotFoundException("Buyer not found for this seller"));
        if (invoices.existsBySupplierIdAndInvoiceNumber(req.supplierId(), req.invoiceNumber())) {
            throw new ConflictException("Invoice number already used by this supplier");
        }

        Invoice invoice = new Invoice();
        invoice.setSupplier(supplier);
        invoice.setBuyer(buyer);
        invoice.setInvoiceNumber(req.invoiceNumber().trim());
        invoice.setInvoiceDate(req.invoiceDate());
        invoice.setDueDate(req.dueDate() != null ? req.dueDate() : req.invoiceDate().plusDays(45));

        for (CreateInvoiceRequest.CreateInvoiceLineRequest lineReq : req.lines()) {
            if (lineReq.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("quantity must be greater than 0");
            }
            Product product = products.findById(lineReq.productId())
                    .orElseThrow(() -> new NotFoundException("Product not found: " + lineReq.productId()));
            if (!product.getSupplier().getId().equals(req.supplierId())) {
                throw new IllegalArgumentException("Product does not belong to this supplier");
            }
            BigDecimal lineTotal = lineReq.quantity().multiply(lineReq.unitPrice()).setScale(2, RoundingMode.HALF_UP);
            InvoiceLine line = new InvoiceLine();
            line.setProduct(product);
            line.setSkuCode(product.getSkuCode());
            line.setDescription(product.getName());
            line.setQuantity(lineReq.quantity());
            line.setUnitPrice(lineReq.unitPrice().setScale(2, RoundingMode.HALF_UP));
            line.setLineTotal(lineTotal);
            invoice.addLine(line);
        }

        return toInvoice(invoices.save(invoice));
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listSupplierInvoices(Long supplierId) {
        requireSupplier(supplierId);
        return invoices.findBySupplierIdOrderByInvoiceDateDesc(supplierId).stream()
                .map(this::toInvoice)
                .toList();
    }

    @Transactional(readOnly = true)
    public BuyerResponse getBuyer(Long buyerId) {
        return toBuyer(requireBuyer(buyerId));
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listBuyerInvoices(Long buyerId) {
        requireBuyer(buyerId);
        return invoices.findByBuyerIdOrderByInvoiceDateDesc(buyerId).stream()
                .map(this::toInvoice)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listInvoicesForBuyerEmail(String email) {
        return invoices.findByBuyer_EmailIgnoreCaseOrderByInvoiceDateDesc(email).stream()
                .map(this::toInvoice)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> listSuppliers() {
        return suppliers.findAll().stream().map(this::toSupplier).toList();
    }

    private Supplier requireSupplier(Long id) {
        return suppliers.findById(id)
                .orElseThrow(() -> new NotFoundException("Supplier not found: " + id));
    }

    private Buyer requireBuyer(Long id) {
        return buyers.findByIdWithSupplier(id)
                .orElseThrow(() -> new NotFoundException("Buyer not found: " + id));
    }

    private SupplierResponse toSupplier(Supplier s) {
        return new SupplierResponse(
                s.getId(),
                s.getName(),
                s.getEmail(),
                s.getGstin(),
                s.isMsme(),
                s.getRegisteredAt(),
                products.countBySupplierId(s.getId()));
    }

    private ProductResponse toProduct(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getSupplier().getId(),
                p.getSkuCode(),
                p.getName(),
                p.getProductType(),
                p.getShelfLifeDays(),
                p.isActive(),
                p.getRegisteredAt());
    }

    private BuyerResponse toBuyer(Buyer b) {
        return new BuyerResponse(
                b.getId(),
                b.getSupplier().getId(),
                b.getName(),
                b.getEmail(),
                b.getGstin(),
                b.getPhone(),
                b.getContactName(),
                b.getBillingAddress(),
                b.getRegisteredAt());
    }

    private InvoiceResponse toInvoice(Invoice invoice) {
        List<InvoiceResponse.InvoiceLineResponse> lines = invoice.getLines().stream()
                .map(line -> new InvoiceResponse.InvoiceLineResponse(
                        line.getId(),
                        line.getProduct() == null ? null : line.getProduct().getId(),
                        line.getSkuCode(),
                        line.getDescription(),
                        line.getQuantity(),
                        line.getUnitPrice(),
                        line.getLineTotal()))
                .toList();
        BigDecimal total = lines.stream()
                .map(InvoiceResponse.InvoiceLineResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate due = invoice.getDueDate();
        long daysOverdue = 0;
        if (due != null && LocalDate.now().isAfter(due)) {
            daysOverdue = ChronoUnit.DAYS.between(due, LocalDate.now());
        }
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getSupplier().getId(),
                invoice.getSupplier().getName(),
                invoice.getBuyer().getId(),
                invoice.getBuyer().getName(),
                invoice.getInvoiceNumber(),
                invoice.getInvoiceDate(),
                due,
                invoice.getStatus().name(),
                total,
                daysOverdue,
                lines);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeGstin(String gstin) {
        String value = trimToNull(gstin);
        if (value == null) {
            return null;
        }
        value = value.replaceAll("\\s+", "").toUpperCase();
        if (value.length() != 15) {
            throw new IllegalArgumentException("GSTIN must be 15 characters");
        }
        return value;
    }
}
