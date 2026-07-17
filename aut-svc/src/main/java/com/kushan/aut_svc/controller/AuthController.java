package com.kushan.aut_svc.controller;

import com.kushan.aut_svc.Model.Admin;
import com.kushan.aut_svc.Model.User;
import com.kushan.aut_svc.dto.AuthResponse;
import com.kushan.aut_svc.dto.LoginRequest;
import com.kushan.aut_svc.repository.AdminRepository;
import com.kushan.aut_svc.repository.UserRepository;
import com.kushan.aut_svc.security.jwt.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(JwtService jwtService,
                          UserRepository userRepository,
                          AdminRepository adminRepository,
                          PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/req/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        User user = userRepository.findByEmail(request.email()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password"));
        }

        String role = user.getRole();
        String token = jwtService.generateToken(user.getEmail(), "ROLE_" + role.toUpperCase());

        return ResponseEntity.ok(new AuthResponse(
                token,
                role,
                user.getId(),
                user.getEmail(),
                (user.getFirstName() != null ? user.getFirstName() + " " : "") + (user.getLastName() != null ? user.getLastName() : "")
        ));
    }

    @PostMapping("/req/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody LoginRequest request) {

        Admin admin = adminRepository.findByEmail(request.email()).orElse(null);

        if (admin == null || !passwordEncoder.matches(request.password(), admin.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid admin credentials"));
        }

        String token = jwtService.generateToken(admin.getEmail(), "ROLE_ADMIN");

        return ResponseEntity.ok(new AuthResponse(
                token,
                "admin",
                admin.getId(),
                admin.getEmail(),
                admin.getName()
        ));
    }
}
