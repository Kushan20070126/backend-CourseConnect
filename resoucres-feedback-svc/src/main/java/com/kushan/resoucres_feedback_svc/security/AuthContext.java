package com.kushan.resoucres_feedback_svc.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
public class AuthContext {
    public String email() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in is required");
        }
        return auth.getName();
    }
    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String wanted = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> wanted.equals(a.getAuthority()));
    }
    public void requireRole(String... roles) {
        email();
        for (String role : roles) if (hasRole(role)) return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
    }
}
