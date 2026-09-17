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
import com.paydue.domain.SupplierBuyer;
import com.paydue.repo.BuyerRepository;
import com.paydue.repo.InvoiceRepository;
import com.paydue.repo.ProductRepository;
import com.paydue.repo.SupplierBuyerRepository;
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
    private final SupplierBuyerRepository links;
    private final ProductRepository products;
    private final InvoiceRepository invoices;

    public PaydueService(
            SupplierRepository suppliers,
            BuyerRepository buyers,
            SupplierBuyerRepository links,
            ProductRepository products,
            InvoiceRepository invoices) {
        this.suppliers = suppliers;
        this.buyers = buyers;
        this.links = links;
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
    public BuyerResponse createBuyer(CreateBuyerRequest req) {
        Buyer b = new Buyer();
        b.setName(req.name().trim());
        b.setEmail(req.email());
        b.setGstin(req.gstin());
        return toBuyer(buyers.save(b));
    }

    @Transactional
    public BuyerResponse linkBuyer(Long supplierId, Long buyerId) {
        Supplier supplier = requireSupplier(supplierId);
        Buyer buyer = buyers.findById(buyerId)
                .orElseThrow(() -> new NotFoundException("Buyer not found: " + buyerId));
        if (links.existsBySupplierIdAndBuyerId(supplierId, buyerId)) {
            throw new ConflictException("Buyer is already linked to this supplier");
        }
        SupplierBuyer link = new SupplierBuyer();
        link.setSupplier(supplier);
        link.setBuyer(buyer);
        links.save(link);
        return toBuyer(buyer);
    }

    @Transactional(readOnly = true)
    public List<BuyerResponse> listMyBuyers(Long supplierId) {
        requireSupplier(supplierId);
        return links.findBySupplierId(supplierId).stream()
                .map(SupplierBuyer::getBuyer)
                .map(this::toBuyer)
                .toList();
    }

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest req) {
        Supplier supplier = requireSupplier(req.supplierId());
        Buyer buyer = buyers.findById(req.buyerId())
                .orElseThrow(() -> new NotFoundException("Buyer not found: " + req.buyerId()));
        if (!links.existsBySupplierIdAndBuyerId(req.supplierId(), req.buyerId())) {
            SupplierBuyer link = new SupplierBuyer();
            link.setSupplier(supplier);
            link.setBuyer(buyer);
            links.save(link);
        }
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
    public List<SupplierResponse> listSuppliers() {
        return suppliers.findAll().stream().map(this::toSupplier).toList();
    }

    @Transactional(readOnly = true)
    public List<BuyerResponse> listBuyers() {
        return buyers.findAll().stream().map(this::toBuyer).toList();
    }

    private Supplier requireSupplier(Long id) {
        return suppliers.findById(id)
                .orElseThrow(() -> new NotFoundException("Supplier not found: " + id));
    }

    private Buyer requireBuyer(Long id) {
        return buyers.findById(id)
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
        return new BuyerResponse(b.getId(), b.getName(), b.getEmail(), b.getGstin(), b.getRegisteredAt());
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
}
