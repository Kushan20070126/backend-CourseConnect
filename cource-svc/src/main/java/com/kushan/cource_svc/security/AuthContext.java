package com.kushan.cource_svc.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Reads the authenticated principal placed in the SecurityContext by
 * JwtAuthenticationFilter. Principal = user email; authority = role
 * (e.g. ROLE_STUDENT, ROLE_LECTURER, ROLE_ADMIN).
 */
@Component
public class AuthContext {

    public Optional<String> email() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();
        return Optional.ofNullable(auth.getName());
    }

    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        String wanted = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(wanted::equals);
    }

    public boolean isStudent() { return hasRole("ROLE_STUDENT"); }
    public boolean isLecturer() { return hasRole("ROLE_LECTURER"); }
    public boolean isAdmin() { return hasRole("ROLE_ADMIN"); }
}
