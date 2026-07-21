package com.kushan.aut_svc.controller;

import com.kushan.aut_svc.Model.Admin;
import com.kushan.aut_svc.Model.Session;
import com.kushan.aut_svc.Model.User;
import com.kushan.aut_svc.dto.AuthResponse;
import com.kushan.aut_svc.dto.LoginRequest;
import com.kushan.aut_svc.repository.AdminRepository;
import com.kushan.aut_svc.repository.SessionRepository;
import com.kushan.aut_svc.repository.UserRepository;
import com.kushan.aut_svc.security.jwt.JwtService;
import com.kushan.aut_svc.util.UserAgentParser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class AuthController {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(JwtService jwtService,
                          UserRepository userRepository,
                          AdminRepository adminRepository,
                          SessionRepository sessionRepository,
                          PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/req/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {

        User user = userRepository.findByEmail(request.email()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password"));
        }
        if (user instanceof com.kushan.aut_svc.Model.Lecturer lecturer
                && !"ACTIVE".equalsIgnoreCase(lecturer.getStatus())) {
            String message = "REJECTED".equalsIgnoreCase(lecturer.getStatus())
                    ? "Your lecturer application was not approved."
                    : "Your lecturer account is awaiting administrator approval.";
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", message));
        }

        String role = user.getRole();
        String token = jwtService.generateToken(user.getEmail(), "ROLE_" + role.toUpperCase());

        String jti = jwtService.getJti(token);
        sessionRepository.save(buildSession(jti, user.getId(), request, httpRequest));

        return ResponseEntity.ok(new AuthResponse(
                token,
                role,
                user.getId(),
                user.getEmail(),
                (user.getFirstName() != null ? user.getFirstName() + " " : "") + (user.getLastName() != null ? user.getLastName() : "")
        ));
    }

    @PostMapping("/req/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {

        Admin admin = adminRepository.findByEmail(request.email()).orElse(null);

        if (admin == null || !passwordEncoder.matches(request.password(), admin.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid admin credentials"));
        }

        String token = jwtService.generateToken(admin.getEmail(), "ROLE_ADMIN");
        String adminJti = jwtService.getJti(token);
        sessionRepository.save(buildSession(adminJti, admin.getId(), request, httpRequest));

        return ResponseEntity.ok(new AuthResponse(
                token,
                "admin",
                admin.getId(),
                admin.getEmail(),
                admin.getName()
        ));
    }

    private Session buildSession(String jti, Long userId, LoginRequest request, HttpServletRequest httpRequest) {
        String userAgent = request.userAgent() != null ? request.userAgent() : httpRequest.getHeader("User-Agent");
        String ip = clientIp(httpRequest);

        Session session = new Session();
        session.setJti(jti);
        session.setUserId(userId);
        session.setIpAddress(ip);
        session.setUserAgent(userAgent);
        session.setDeviceType(UserAgentParser.deviceType(userAgent));
        session.setOs(UserAgentParser.os(userAgent));
        session.setBrowser(UserAgentParser.browser(userAgent));
        LocalDateTime now = LocalDateTime.now();
        session.setCreatedAt(now);
        session.setLastSeen(now);
        session.setExpiresAt(LocalDateTime.now().plusSeconds(jwtService.getExpirationSeconds()));
        session.setActive(true);
        return session;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
