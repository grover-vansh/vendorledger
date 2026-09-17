package com.paydue.security;

import com.paydue.domain.AppUser;
import com.paydue.domain.UserRole;
import com.paydue.web.error.ForbiddenException;
import com.paydue.web.error.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public AppUser require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser user)) {
            throw new UnauthorizedException("Login required");
        }
        return user;
    }

    public void requireSellerOf(Long supplierId) {
        AppUser user = require();
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        if (user.getRole() == UserRole.SELLER
                && user.getSupplier() != null
                && user.getSupplier().getId().equals(supplierId)) {
            return;
        }
        throw new ForbiddenException("Not allowed to act as this seller");
    }

    public AppUser requireBuyer() {
        AppUser user = require();
        if (user.getRole() != UserRole.BUYER) {
            throw new ForbiddenException("Buyer only");
        }
        return user;
    }

    public void requireCanViewBuyer(Long ownerSupplierId, String buyerEmail) {
        AppUser user = require();
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        if (user.getRole() == UserRole.SELLER
                && user.getSupplier() != null
                && user.getSupplier().getId().equals(ownerSupplierId)) {
            return;
        }
        if (user.getRole() == UserRole.BUYER
                && buyerEmail != null
                && buyerEmail.equalsIgnoreCase(user.getEmail())) {
            return;
        }
        throw new ForbiddenException("Not allowed to view this buyer");
    }

    public void requireAdmin() {
        if (require().getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Admin only");
        }
    }
}
