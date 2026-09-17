package com.paydue.web;

import com.paydue.api.dto.BuyerResponse;
import com.paydue.api.dto.CreateBuyerRequest;
import com.paydue.api.dto.CreateInvoiceRequest;
import com.paydue.api.dto.CreateProductRequest;
import com.paydue.api.dto.CreateSupplierRequest;
import com.paydue.api.dto.InvoiceResponse;
import com.paydue.api.dto.ProductResponse;
import com.paydue.api.dto.SupplierResponse;
import com.paydue.api.dto.UserSummaryResponse;
import com.paydue.service.AuthService;
import com.paydue.service.PaydueService;
import com.paydue.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PaydueController {

    private final PaydueService service;
    private final AuthService authService;
    private final CurrentUser currentUser;

    public PaydueController(PaydueService service, AuthService authService, CurrentUser currentUser) {
        this.service = service;
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @PostMapping("/suppliers")
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse createSupplier(@Valid @RequestBody CreateSupplierRequest request) {
        currentUser.requireAdmin();
        return service.createSupplier(request);
    }

    @GetMapping("/suppliers")
    public List<SupplierResponse> listSuppliers() {
        currentUser.requireAdmin();
        return service.listSuppliers();
    }

    @GetMapping("/admin/users")
    public List<UserSummaryResponse> listUsers() {
        currentUser.requireAdmin();
        return authService.listUsers();
    }

    @GetMapping("/suppliers/{supplierId}")
    public SupplierResponse getSupplier(@PathVariable Long supplierId) {
        currentUser.requireSellerOf(supplierId);
        return service.getSupplier(supplierId);
    }

    @PostMapping("/suppliers/{supplierId}/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse addProduct(
            @PathVariable Long supplierId,
            @Valid @RequestBody CreateProductRequest request) {
        currentUser.requireSellerOf(supplierId);
        return service.addProduct(supplierId, request);
    }

    @GetMapping("/suppliers/{supplierId}/products")
    public List<ProductResponse> listProducts(@PathVariable Long supplierId) {
        currentUser.requireSellerOf(supplierId);
        return service.listProducts(supplierId);
    }

    @PostMapping("/buyers")
    @ResponseStatus(HttpStatus.CREATED)
    public BuyerResponse createBuyer(@Valid @RequestBody CreateBuyerRequest request) {
        currentUser.require();
        return service.createBuyer(request);
    }

    @GetMapping("/buyers")
    public List<BuyerResponse> listBuyers() {
        currentUser.requireSellerOrAdmin();
        return service.listBuyers();
    }

    @GetMapping("/buyers/{buyerId}")
    public BuyerResponse getBuyer(@PathVariable Long buyerId) {
        currentUser.requireBuyerOf(buyerId);
        return service.getBuyer(buyerId);
    }

    @GetMapping("/buyers/{buyerId}/invoices")
    public List<InvoiceResponse> listBuyerInvoices(@PathVariable Long buyerId) {
        currentUser.requireBuyerOf(buyerId);
        return service.listBuyerInvoices(buyerId);
    }

    @PostMapping("/suppliers/{supplierId}/buyers/{buyerId}")
    @ResponseStatus(HttpStatus.CREATED)
    public BuyerResponse linkBuyer(@PathVariable Long supplierId, @PathVariable Long buyerId) {
        currentUser.requireSellerOf(supplierId);
        return service.linkBuyer(supplierId, buyerId);
    }

    @GetMapping("/suppliers/{supplierId}/buyers")
    public List<BuyerResponse> listMyBuyers(@PathVariable Long supplierId) {
        currentUser.requireSellerOf(supplierId);
        return service.listMyBuyers(supplierId);
    }

    @PostMapping("/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        currentUser.requireSellerOf(request.supplierId());
        return service.createInvoice(request);
    }

    @GetMapping("/suppliers/{supplierId}/invoices")
    public List<InvoiceResponse> listInvoices(@PathVariable Long supplierId) {
        currentUser.requireSellerOf(supplierId);
        return service.listSupplierInvoices(supplierId);
    }
}
