package com.kushan.aut_svc.dto;

public record AuthResponse(
        String token,
        String role,
        Long userId,
        String email,
        String name
) {
}
