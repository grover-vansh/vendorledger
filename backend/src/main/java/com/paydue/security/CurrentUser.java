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

    public void requireBuyerOf(Long buyerId) {
        AppUser user = require();
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        if (user.getRole() == UserRole.BUYER
                && user.getBuyer() != null
                && user.getBuyer().getId().equals(buyerId)) {
            return;
        }
        throw new ForbiddenException("Not allowed to act as this buyer");
    }

    public void requireAdmin() {
        if (require().getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Admin only");
        }
    }
}
