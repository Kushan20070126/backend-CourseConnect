package com.kushan.aut_svc.service;

import com.kushan.aut_svc.Model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    // email (lower-cased) -> pending registration. In-memory; swap for a
    // shared store (Redis) if you run multiple auth-svc instances.
    private final Map<String, PendingRegistration> store = new ConcurrentHashMap<>();
    private final long expiryMs;
    private final SecureRandom random = new SecureRandom();

    public OtpService(@Value("${otp.expiry-ms:300000}") long expiryMs) {
        this.expiryMs = expiryMs;
    }

    public String generateAndStore(User user) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        store.put(user.getEmail().toLowerCase(),
                new PendingRegistration(user, code, System.currentTimeMillis() + expiryMs));
        return code;
    }

    public String resend(String email) {
        PendingRegistration pending = store.get(email.toLowerCase());
        if (pending == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No pending registration for this email");
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        store.put(email.toLowerCase(),
                new PendingRegistration(pending.user, code, System.currentTimeMillis() + expiryMs));
        return code;
    }

    public User verify(String email, String code) {
        PendingRegistration pending = store.get(email.toLowerCase());
        if (pending == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No pending registration for this email");
        }
        if (pending.expiry < System.currentTimeMillis()) {
            store.remove(email.toLowerCase());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP has expired. Please register again.");
        }
        if (!pending.code.equals(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code");
        }
        store.remove(email.toLowerCase());
        return pending.user;
    }

    private static class PendingRegistration {
        final User user;
        final String code;
        final long expiry;

        PendingRegistration(User user, String code, long expiry) {
            this.user = user;
            this.code = code;
            this.expiry = expiry;
        }
    }
}
