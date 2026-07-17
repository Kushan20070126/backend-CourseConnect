package com.kushan.aut_svc.controller;

import com.kushan.aut_svc.Model.User;
import com.kushan.aut_svc.dto.AuthResponse;
import com.kushan.aut_svc.repository.UserRepository;
import com.kushan.aut_svc.security.jwt.JwtService;
import com.kushan.aut_svc.service.EmailService;
import com.kushan.aut_svc.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
public class RegistrationController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private OtpService otpService;
    @Autowired
    private EmailService emailService;

    // Step 1 — capture details, generate + email a 6-digit OTP.
    // The account is NOT created until the OTP is verified.
    @PostMapping(value = "/req/signup", consumes = "application/json")
    public ResponseEntity<?> initiateSignup(@RequestBody User user) {

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        String otp = otpService.generateAndStore(user);
        emailService.sendOtp(user.getEmail(), otp);

        return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "message", "A 6-digit verification code has been sent to your email."
        ));
    }

    // Step 2 — verify OTP, create the account, and return a JWT.
    @PostMapping(value = "/req/verify-otp", consumes = "application/json")
    public AuthResponse verifyOtp(@RequestBody Map<String, String> body) {

        String email = body.get("email");
        String otp = body.get("otp");
        if (email == null || otp == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and OTP are required");
        }

        User user = otpService.verify(email, otp);
        // New lecturers await admin approval; students are active immediately.
        if (user instanceof com.kushan.aut_svc.Model.Lecturer lecturer) {
            lecturer.setStatus("PENDING");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User saved = userRepository.save(user);

        String role = saved.getRole();
        String token = jwtService.generateToken(saved.getEmail(), "ROLE_" + role.toUpperCase());

        return new AuthResponse(
                token,
                role,
                saved.getId(),
                saved.getEmail(),
                (saved.getFirstName() != null ? saved.getFirstName() + " " : "") +
                        (saved.getLastName() != null ? saved.getLastName() : "")
        );
    }

    // Resend a fresh OTP to the pending email.
    @PostMapping(value = "/req/resend-otp", consumes = "application/json")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> body) {

        String email = body.get("email");
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        String otp = otpService.resend(email);
        emailService.sendOtp(email, otp);

        return ResponseEntity.ok(Map.of(
                "email", email,
                "message", "A new verification code has been sent."
        ));
    }
}
