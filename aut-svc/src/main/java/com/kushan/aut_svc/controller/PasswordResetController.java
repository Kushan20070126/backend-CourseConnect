package com.kushan.aut_svc.controller;

import com.kushan.aut_svc.Model.User;
import com.kushan.aut_svc.repository.UserRepository;
import com.kushan.aut_svc.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class PasswordResetController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetController(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // Step 1 — generate a reset code and email it. Always returns 200 so the
    // response doesn't reveal whether an account exists for the given email.
    @PostMapping(value = "/req/forgot-password", consumes = "application/json")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (email != null && !email.isBlank()) {
            userRepository.findByEmail(email).ifPresent(user -> {
                String code = String.format("%06d", random.nextInt(1_000_000));
                user.setResetOtp(code);
                user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(10));
                userRepository.save(user);
                emailService.sendOtp(user.getEmail(), code);
            });
        }

        return ResponseEntity.ok(Map.of(
                "message", "If that email exists, a reset code has been sent."
        ));
    }

    // Step 2 — verify the code and set a new password.
    @PostMapping(value = "/req/reset-password", consumes = "application/json")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");
        String newPassword = body.get("newPassword");

        if (email == null || email.isBlank()
                || otp == null || otp.isBlank()
                || newPassword == null || newPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email, code and new password are required");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request"));

        if (user.getResetOtp() == null
                || !user.getResetOtp().equals(otp)
                || user.getResetOtpExpiry() == null
                || user.getResetOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired code");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetOtp(null);
        user.setResetOtpExpiry(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
    }
}
