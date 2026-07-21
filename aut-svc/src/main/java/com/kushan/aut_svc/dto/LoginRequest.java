package com.kushan.aut_svc.dto;

public record LoginRequest(String email, String password, Boolean remember, String userAgent) {
}
