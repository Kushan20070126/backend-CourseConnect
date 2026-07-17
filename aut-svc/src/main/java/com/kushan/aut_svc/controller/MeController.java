package com.kushan.aut_svc.controller;

import com.kushan.aut_svc.Model.Admin;
import com.kushan.aut_svc.Model.User;
import com.kushan.aut_svc.repository.AdminRepository;
import com.kushan.aut_svc.repository.UserRepository;
import com.kushan.aut_svc.security.jwt.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
public class MeController {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    public MeController(JwtService jwtService,
                        UserRepository userRepository,
                        AdminRepository adminRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
    }

    @GetMapping("/req/me")
    public Map<String, Object> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        }

        String token = authHeader.substring(7);
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

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("role", user.getRole());
        result.put("id", user.getId());
        result.put("email", user.getEmail());
        result.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        result.put("lastName", user.getLastName() != null ? user.getLastName() : "");
        result.put("age", user.getAge() != null ? user.getAge() : 0);

        if (user instanceof com.kushan.aut_svc.Model.Student s) {
            result.put("educationLevel", s.getEducationLevel());
            result.put("interest", s.getInterest());
            result.put("goal", s.getGoal());
        } else if (user instanceof com.kushan.aut_svc.Model.Lecturer l) {
            result.put("title", l.getTitle());
            result.put("experience", l.getExperience());
            result.put("area", l.getArea());
            result.put("bio", l.getBio());
        }

        return result;
    }
}
