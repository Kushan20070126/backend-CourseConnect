package com.kushan.aut_svc.controller;

import com.kushan.aut_svc.Model.Admin;
import com.kushan.aut_svc.Model.Lecturer;
import com.kushan.aut_svc.Model.Student;
import com.kushan.aut_svc.Model.User;
import com.kushan.aut_svc.dto.UpdateMeRequest;
import com.kushan.aut_svc.repository.AdminRepository;
import com.kushan.aut_svc.repository.UserRepository;
import com.kushan.aut_svc.security.jwt.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
public class MeController {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public MeController(JwtService jwtService,
                        UserRepository userRepository,
                        AdminRepository adminRepository,
                        PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/req/me")
    public Map<String, Object> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {

        String token = requireToken(authHeader);
        String role = jwtService.getRole(token);
        String email = jwtService.getSubject(token);

        if ("ROLE_ADMIN".equals(role)) {
            Admin admin = adminRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found"));
            return Map.of(
                    "role", "admin",
                    "id", admin.getId(),
                    "email", admin.getEmail(),
                    "name", admin.getName() != null ? admin.getName() : ""
            );
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        return toProfile(user);
    }

    // Update the currently authenticated user's own profile.
    // Only non-null fields in the request are applied. Email/password changes
    // are intentionally not handled here.
    @PutMapping("/req/me")
    @Transactional
    public Map<String, Object> updateMe(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdateMeRequest body) {

        String token = requireToken(authHeader);
        String role = jwtService.getRole(token);
        String email = jwtService.getSubject(token);

        if ("ROLE_ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Admin profiles cannot be updated through this endpoint");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Common fields
        if (body.getFirstName() != null) user.setFirstName(body.getFirstName());
        if (body.getLastName() != null) user.setLastName(body.getLastName());
        if (body.getAge() != null) user.setAge(body.getAge());

        // Role-specific fields
        if (user instanceof Student s) {
            if (body.getEducationLevel() != null) s.setEducationLevel(body.getEducationLevel());
            if (body.getInterest() != null) s.setInterest(body.getInterest());
            if (body.getGoal() != null) s.setGoal(body.getGoal());
        } else if (user instanceof Lecturer l) {
            if (body.getTitle() != null) l.setTitle(body.getTitle());
            if (body.getExperience() != null) l.setExperience(body.getExperience());
            if (body.getArea() != null) l.setArea(body.getArea());
            if (body.getBio() != null) l.setBio(body.getBio());
        }

        User saved = userRepository.save(user);
        return toProfile(saved);
    }

    // Change the currently authenticated user's password. Requires the current
    // password for verification.
    @PutMapping("/req/me/password")
    @Transactional
    public Map<String, Object> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> body) {

        String token = requireToken(authHeader);
        String role = jwtService.getRole(token);
        String email = jwtService.getSubject(token);

        if ("ROLE_ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Admin passwords cannot be changed through this endpoint");
        }

        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (currentPassword == null || currentPassword.isBlank()
                || newPassword == null || newPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Current and new password are required");
        }
        if (newPassword.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "New password must be at least 6 characters");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return Map.of("message", "Password updated successfully.");
    }

    // Permanently delete the currently authenticated user's own account.
    @DeleteMapping("/req/me")
    @Transactional
    public Map<String, Object> deleteMe(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String token = requireToken(authHeader);
        String role = jwtService.getRole(token);
        String email = jwtService.getSubject(token);

        if ("ROLE_ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Admin accounts cannot be deleted through this endpoint");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        userRepository.delete(user);

        return Map.of("message", "Your account has been deleted.");
    }

    // --- helpers ---

    private String requireToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        }
        return authHeader.substring(7);
    }

    private Map<String, Object> toProfile(User user) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("role", user.getRole());
        result.put("id", user.getId());
        result.put("email", user.getEmail());
        result.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        result.put("lastName", user.getLastName() != null ? user.getLastName() : "");
        result.put("age", user.getAge() != null ? user.getAge() : 0);

        if (user instanceof Student s) {
            result.put("educationLevel", s.getEducationLevel());
            result.put("interest", s.getInterest());
            result.put("goal", s.getGoal());
        } else if (user instanceof Lecturer l) {
            result.put("title", l.getTitle());
            result.put("experience", l.getExperience());
            result.put("area", l.getArea());
            result.put("bio", l.getBio());
        }

        return result;
    }
}
