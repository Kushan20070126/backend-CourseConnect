package com.kushan.aut_svc.controller;

import com.kushan.aut_svc.Model.Session;
import com.kushan.aut_svc.Model.User;
import com.kushan.aut_svc.repository.SessionRepository;
import com.kushan.aut_svc.repository.UserRepository;
import com.kushan.aut_svc.security.jwt.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class SessionController {

    private final JwtService jwtService;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    public SessionController(JwtService jwtService,
                             SessionRepository sessionRepository,
                             UserRepository userRepository) {
        this.jwtService = jwtService;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/req/sessions")
    public Map<String, Object> listSessions(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = requireToken(authHeader);
        String email = jwtService.getSubject(token);
        String jti = jwtService.getJti(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        List<Session> sessions = sessionRepository.findByUserIdAndActiveTrueOrderByLastSeenDesc(user.getId());

        List<Map<String, Object>> devices = sessions.stream()
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", s.getId());
                    m.put("current", s.getJti().equals(jti));
                    m.put("ip", s.getIpAddress());
                    m.put("device", s.getDeviceType());
                    m.put("os", s.getOs());
                    m.put("browser", s.getBrowser());
                    m.put("createdAt", s.getCreatedAt());
                    m.put("lastSeen", s.getLastSeen());
                    return m;
                })
                .collect(Collectors.toList());

        return Map.of(
                "currentJti", jti,
                "sessions", devices,
                "count", sessions.size()
        );
    }

    @PostMapping("/req/sessions/logout-others")
    public Map<String, Object> logoutOtherSessions(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = requireToken(authHeader);
        String email = jwtService.getSubject(token);
        String jti = jwtService.getJti(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        List<Session> sessions = sessionRepository.findByUserIdAndActiveTrueOrderByLastSeenDesc(user.getId());
        int revoked = 0;
        for (Session s : sessions) {
            if (!s.getJti().equals(jti)) {
                s.setActive(false);
                sessionRepository.save(s);
                revoked++;
            }
        }

        return Map.of(
                "message", revoked > 0
                        ? "Signed out of " + revoked + " other device(s)."
                        : "No other devices were signed in.",
                "revoked", revoked
        );
    }

    private String requireToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        }
        return authHeader.substring(7);
    }
}
