package com.kushan.cource_svc.controller;

import com.kushan.cource_svc.security.AuthContext;
import com.kushan.cource_svc.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/req")
public class PaymentController {

    private final PaymentService paymentService;
    private final AuthContext authContext;

    public PaymentController(PaymentService paymentService, AuthContext authContext) {
        this.paymentService = paymentService;
        this.authContext = authContext;
    }

    /** Stripe calls this (no JWT). Signature is verified inside the service. */
    @PostMapping("/payments/webhook")
    public ResponseEntity<Void> webhook(HttpServletRequest request) throws IOException {
        String sig = request.getHeader("Stripe-Signature");
        byte[] body = request.getInputStream().readAllBytes();
        paymentService.handleWebhook(body, sig);
        return ResponseEntity.ok().build();
    }

    /** Dev fallback used by the frontend after Stripe redirects back. */
    @GetMapping("/payments/confirm")
    public Map<String, Object> confirm(@RequestParam String sessionId) {
        if (!authContext.hasRole("ROLE_STUDENT")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Students only");
        }
        return paymentService.confirm(sessionId, authContext.email().orElseThrow());
    }
}
