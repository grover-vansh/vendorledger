package com.paydue.service;

import com.paydue.api.dto.AuthResponse;
import com.paydue.api.dto.CreateSupplierRequest;
import com.paydue.api.dto.LoginRequest;
import com.paydue.api.dto.RegisterRequest;
import com.paydue.api.dto.UserSummaryResponse;
import com.paydue.domain.AppUser;
import com.paydue.domain.UserRole;
import com.paydue.repo.AppUserRepository;
import com.paydue.repo.SupplierRepository;
import com.paydue.security.JwtService;
import com.paydue.web.error.ConflictException;
import com.paydue.web.error.UnauthorizedException;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository users;
    private final PaydueService paydue;
    private final SupplierRepository suppliers;
    private final PasswordEncoder passwords;
    private final JwtService jwt;

    public AuthService(
            AppUserRepository users,
            PaydueService paydue,
            SupplierRepository suppliers,
            PasswordEncoder passwords,
            JwtService jwt) {
        this.users = users;
        this.paydue = paydue;
        this.suppliers = suppliers;
        this.passwords = passwords;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already registered");
        }

        if (req.role() == UserRole.SELLER) {
            if (users.existsBySupplier_NameIgnoreCase(req.name())) {
                throw new ConflictException("Seller already registered");
            }
        }

        if (req.role() == UserRole.BUYER) {
            String company = req.name() == null ? "" : req.name().trim();
            if (users.existsByRoleAndCompanyNameIgnoreCase(UserRole.BUYER, company)) {
                throw new ConflictException("Buyer already registered");
            }
        }
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwords.encode(req.password()));
        user.setRole(req.role());

        if (req.role() == UserRole.SELLER) {
            String company = req.name() == null || req.name().isBlank() ? email : req.name().trim();
            var supplier = paydue.createSupplier(new CreateSupplierRequest(company, email, null, true));
            user.setSupplier(suppliers.getReferenceById(supplier.id()));
            user.setCompanyName(company);
        } else if (req.role() == UserRole.BUYER) {
            String company = req.name() == null || req.name().isBlank() ? email : req.name().trim();
            user.setCompanyName(company);
        } else if (req.role() != UserRole.ADMIN) {
            throw new IllegalArgumentException("role must be BUYER, SELLER, or ADMIN");
        }

        AppUser saved = users.save(user);
        return toAuth(saved, jwt.createToken(saved));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        AppUser user = users.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!passwords.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        return toAuth(user, jwt.createToken(user));
    }

    public AuthResponse me(AppUser user) {
        return toAuth(user, jwt.createToken(user));
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listUsers() {
        return users.findAllWithCompanies().stream()
                .map(AuthService::toSummary)
                .toList();
    }

    private AuthResponse toAuth(AppUser user, String token) {
        Long supplierId = user.getSupplier() == null ? null : user.getSupplier().getId();
        return AuthResponse.bearer(
                token,
                user.getId(),
                user.getEmail(),
                user.getRole(),
                supplierId,
                null,
                companyName(user));
    }

    private static UserSummaryResponse toSummary(AppUser user) {
        Long supplierId = user.getSupplier() == null ? null : user.getSupplier().getId();
        return new UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                supplierId,
                null,
                companyName(user));
    }

    private static String companyName(AppUser user) {
        if (user.getCompanyName() != null && !user.getCompanyName().isBlank()) {
            return user.getCompanyName();
        }
        if (user.getSupplier() != null) {
            return user.getSupplier().getName();
        }
        return null;
    }
}
